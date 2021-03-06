package fitnesse.wiki.fs;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.*;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.*;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import fitnesse.wiki.fs.exceptions.NotEnoughWorkspaceItemSetsException;
import fitnesse.wiki.fs.exceptions.TooManyWorkspaceItemSetsException;
import fitnesse.wiki.fs.exceptions.TooManyWorkspaceItemsException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class TfsWrapper {
    private final String tfsServerUri;
    private VersionControlClient versionControlClient;
    private PersistenceStoreProvider persistenceStoreProvider;

    public TfsWrapper(String tfsServerUri) {
        this.tfsServerUri = tfsServerUri;
        initialise();
    }

    private void initialise() {
        TFSTeamProjectCollection tpc;
        try {
            tpc = new TFSTeamProjectCollection(new URI(tfsServerUri), new DefaultNTCredentials());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(String.format("Could not connect to TFS project collection: %s", tfsServerUri));
        }

        persistenceStoreProvider = tpc.getPersistenceStoreProvider();
        versionControlClient = tpc.getVersionControlClient();
    }

    public File getRepositoryFile(File localFile) {
        Path normalisedFilePath = getNormalisedFilePath(localFile);
        String filePath = normalisedFilePath.toString();
        WorkspaceItem workspaceItem = getWorkspaceItem(filePath);
        return workspaceItem == null ? null : workspaceItem.downloadFileToTempLocation(versionControlClient, normalisedFilePath.getFileName().toString());
    }

    public void delete(File localFile) {
        String filePath = getNormalisedFilePath(localFile).toString();
        Workspace workspace = getWorkspace(filePath);
        if (workspace != null) {
            NonFatalErrorListener nonFatalEventListener = new NonFatalErrorListener() {
                @Override
                public void onNonFatalError(NonFatalErrorEvent nonFatalErrorEvent) {
                    System.out.println(nonFatalErrorEvent.getMessage());
                }
            };
            versionControlClient.getEventEngine().addNonFatalErrorListener(nonFatalEventListener);
            Workstation.getCurrent(persistenceStoreProvider).ensureUpdateWorkspaceInfoCache(versionControlClient, workspace.getOwnerName());
            String parentPath = Paths.get(filePath).getParent().toAbsolutePath().toString();
            workspace.pendDelete(new String[]{filePath}, RecursionType.NONE, LockLevel.NONE, GetOptions.NONE, PendChangesOptions.NONE);
            int failures = checkinPendingChanges(workspace, "Deleting file " + filePath + " to TFS");
            if (failures > 0) System.out.println(String.format("Failures checking-in %s: %d", parentPath, failures));
        } else {
            System.out.println(String.format("Could not find workspace to delete %s from.", filePath));
        }
    }

    public void checkinPending(File localFile) {
        String filePath = getNormalisedFilePath(localFile).toString();
        Workspace workspace = getWorkspace(filePath);
        if (workspace != null) {
            Workstation.getCurrent(persistenceStoreProvider).ensureUpdateWorkspaceInfoCache(versionControlClient, workspace.getOwnerName());
            workspace.pendEdit(new String[]{filePath}, RecursionType.NONE, LockLevel.NONE, null, GetOptions.NONE, PendChangesOptions.NONE);
            int failures = checkinPendingChanges(workspace, String.format("checking in file %s", filePath));
            if (failures > 0) System.out.println(String.format("Failures checking-in: %d", failures));
        } else {
            System.out.println(String.format("Could not find workspace to check-in %s to.", filePath));
        }
    }

    public List<Changeset> getHistory(File localFile) {
        Path normalisedFilePath = getNormalisedFilePath(localFile);
        String filePath = normalisedFilePath.toString();
        Changeset[] changesets = versionControlClient.queryHistory(filePath, LatestVersionSpec.INSTANCE, 0,
                RecursionType.NONE, null, null, LatestVersionSpec.INSTANCE, Integer.MAX_VALUE, true, false, false, false);

        return Arrays.asList(changesets);
    }

    public void addToRepository(File localFile) {
        String filePath = getNormalisedFilePath(localFile).toString();
        Workspace workspace = getWorkspace(filePath);
        if (workspace != null) {
            NonFatalErrorListener nonFatalEventListener = new NonFatalErrorListener() {
                @Override
                public void onNonFatalError(NonFatalErrorEvent nonFatalErrorEvent) {
                    System.out.println(nonFatalErrorEvent.getMessage());
                }
            };
            versionControlClient.getEventEngine().addNonFatalErrorListener(nonFatalEventListener);
            Workstation.getCurrent(persistenceStoreProvider).ensureUpdateWorkspaceInfoCache(versionControlClient, workspace.getOwnerName());
            String parentPath = Paths.get(filePath).getParent().toAbsolutePath().toString();
            workspace.pendAdd(new String[]{filePath}, false, null, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);
            int failures = checkinPendingChanges(workspace, "Adding file " + filePath + " to TFS");
            if (failures > 0) System.out.println(String.format("Failures checking-in %s: %d", parentPath, failures));
        } else {
            System.out.println(String.format("Could not find workspace to add %s to.", filePath));
        }
    }

    private Path getNormalisedFilePath(File file) {
        return file.toPath().toAbsolutePath().normalize();
    }

    private WorkspaceItem getWorkspaceItem(String filePath) {
        WorkspaceItemSet[] workspaceItemSets = getWorkspaceItemSets(filePath);
        if (workspaceItemSets == null) return null;

        WorkspaceItem[] workspaceItems = workspaceItemSets[0].getItems();
        if (workspaceItems == null || workspaceItems.length == 0) return null;
        if (workspaceItems.length > 1) throw new TooManyWorkspaceItemsException(workspaceItemSets[0], workspaceItems);

        return workspaceItems[0];
    }

    private Workspace getWorkspace(String filePath) {
        Workspace[] repositoryWorkspaces = getRepositoryWorkspaces();
        for (Workspace workspace : repositoryWorkspaces) {
            String mappedServerPath = workspace.getMappedServerPath(filePath);
            if (mappedServerPath != null) return workspace;
        }
        System.out.println(String.format("Could not find a workspace to add %s to TFS", filePath));
        return null;
    }

    private WorkspaceItemSet[] getWorkspaceItemSets(String filePath) {
        Workspace[] repositoryWorkspaces = getRepositoryWorkspaces();
        String mappedServerPath = null;
        Workspace foundWorkspace = null;
        for (Workspace workspace : repositoryWorkspaces) {
            foundWorkspace = workspace;
            mappedServerPath = workspace.getMappedServerPath(filePath);
            if (mappedServerPath != null) break;
        }

        if (mappedServerPath == null) return null;

        ItemSpec[] itemSpecs = {new ItemSpec(mappedServerPath, RecursionType.NONE)};
        WorkspaceItemSet[] workspaceItemSets = foundWorkspace.getItems(itemSpecs, DeletedState.NON_DELETED, ItemType.ANY, true, GetItemsOptions.NONE);
        if (workspaceItemSets == null || workspaceItemSets.length == 0)
            throw new NotEnoughWorkspaceItemSetsException(mappedServerPath);
        if (workspaceItemSets.length > 1) throw new TooManyWorkspaceItemSetsException(workspaceItemSets);

        return workspaceItemSets;
    }

    private Workspace[] getRepositoryWorkspaces() {
        return versionControlClient.getRepositoryWorkspaces(null, null, null);
    }

    private int checkinPendingChanges(final Workspace workspace, final String comment) {
        PendingSet pendingSet = workspace.getPendingChanges();
        int cs = 0;

        if (pendingSet != null) {
            PendingChange[] pendingChanges = pendingSet.getPendingChanges();
            if (pendingChanges != null) {
                System.out.println(String.format("Number of pending changes:%d", pendingChanges.length));
                cs = workspace.checkIn(pendingChanges, comment);
            } else {
                System.out.println("Pending changes is null.");
            }
        } else {
            System.out.println("Pending set is null.");
        }

        return cs;
    }
}
