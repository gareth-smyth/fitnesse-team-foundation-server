package fitnesse.wiki.fs;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.*;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.*;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import fitnesse.wiki.fs.exceptions.NotEnoughWorkspaceItemSetsException;
import fitnesse.wiki.fs.exceptions.TooManyWorkspaceItemSetsException;
import fitnesse.wiki.fs.exceptions.TooManyWorkspaceItemsException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Date;

public class TfsWrapper {
    private final String tfsServerUri;
    private VersionControlClient versionControlClient;
    private PersistenceStoreProvider persistenceStoreProvider;

    public TfsWrapper(String tfsServerUri) {
        this.tfsServerUri = tfsServerUri;
    }

    public void initialise(){
        versionControlClient = getVersionControlClient();
    }

    public WorkspaceItem getWorkspaceItem(String filePath) {
        WorkspaceItemSet[] workspaceItemSets = getWorkspaceItemSets(filePath);
        if (workspaceItemSets == null) return null;

        WorkspaceItem[] workspaceItems = workspaceItemSets[0].getItems();
        if (workspaceItems == null || workspaceItems.length == 0) return null;
        if (workspaceItems.length > 1) throw new TooManyWorkspaceItemsException(workspaceItemSets[0], workspaceItems);

        return workspaceItems[0];
    }

    public Workspace getWorkspace(String filePath){
        Workspace[] repositoryWorkspaces = getRepositoryWorkspaces();
        for (Workspace workspace : repositoryWorkspaces) {
            String mappedServerPath = workspace.getMappedServerPath(filePath);
            if (mappedServerPath != null) return workspace;
        }
        System.out.println(String.format("Could not find a workspace to add %s to TFS", filePath));
        return null;
    }

    public WorkspaceItemSet[] getWorkspaceItemSets(String filePath) {
        Workspace[] repositoryWorkspaces = getRepositoryWorkspaces();
        String mappedServerPath = null;
        Workspace foundWorkspace = null;
        for (Workspace workspace : repositoryWorkspaces) {
            foundWorkspace = workspace;
            mappedServerPath = workspace.getMappedServerPath(filePath);
            if(mappedServerPath!=null) break;
        }

        if(mappedServerPath==null) return null;

        ItemSpec[] itemSpecs = {new ItemSpec(mappedServerPath, RecursionType.NONE)};
        WorkspaceItemSet[] workspaceItemSets = foundWorkspace.getItems(itemSpecs, DeletedState.ANY, ItemType.ANY, true, GetItemsOptions.NONE);
        if (workspaceItemSets == null || workspaceItemSets.length == 0)
            throw new NotEnoughWorkspaceItemSetsException(mappedServerPath);
        if (workspaceItemSets.length > 1) throw new TooManyWorkspaceItemSetsException(workspaceItemSets);

        return workspaceItemSets;
    }

    public Workspace[] getRepositoryWorkspaces(){
        return versionControlClient.getRepositoryWorkspaces(null, null, null);
    }

    private VersionControlClient getVersionControlClient() {
        TFSTeamProjectCollection tpc;
        try {
            tpc = new TFSTeamProjectCollection(new URI(tfsServerUri), new DefaultNTCredentials());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not connect to TFS project collection:" + tfsServerUri);
        }

        persistenceStoreProvider = tpc.getPersistenceStoreProvider();
        return tpc.getVersionControlClient();
    }

    public File getRepositoryFile(WorkspaceItem workspaceItem, String name) {
        return workspaceItem.downloadFileToTempLocation(versionControlClient, name);
    }

    public void addToRepository(String filePath) {
        Workspace workspace = getWorkspace(filePath);
        if(workspace!=null) {

            NonFatalErrorListener nonFatalEventListener = new NonFatalErrorListener() {
                @Override
                public void onNonFatalError(NonFatalErrorEvent nonFatalErrorEvent) {
                    System.out.println(nonFatalErrorEvent.getMessage());
                }
            };
            versionControlClient.getEventEngine().addNonFatalErrorListener(nonFatalEventListener);
            Workstation.getCurrent(persistenceStoreProvider).ensureUpdateWorkspaceInfoCache(versionControlClient, workspace.getOwnerName());
            String parentPath = Paths.get(filePath).getParent().toAbsolutePath().toString();
            workspace.pendAdd(new String[]{ filePath}, true, null, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);
            int failures = checkinPendingChanges(workspace, "Adding file " + filePath + " to TFS");
            System.out.println(String.format("Failures checking-in "+parentPath+": %d", failures));
        }else{
            System.out.println(String.format("Could not find workspace to add %s to.", filePath));
        }
    }

    public int checkinPendingChanges(final Workspace workspace, final String comment)
    {
        PendingSet pendingSet = workspace.getPendingChanges();
        int cs = 0;

        if (pendingSet != null)
        {
            PendingChange[] pendingChanges = pendingSet.getPendingChanges();
            if (pendingChanges != null)
            {
                System.out.println(String.format("Number of pending changes:%d", pendingChanges.length));
                cs = workspace.checkIn(pendingChanges, comment);
            }else{
                System.out.println("Pending changes is null.");
            }
        }else{
            System.out.println("Pending set is null.");
        }

        return cs;
    }

    public void checkinPending(String filePath) {
        Workspace workspace = getWorkspace(filePath);
        if(workspace!=null) {
            Workstation.getCurrent(persistenceStoreProvider).ensureUpdateWorkspaceInfoCache(versionControlClient, workspace.getOwnerName());
            workspace.pendEdit(new String[]{ filePath}, RecursionType.NONE, LockLevel.UNCHANGED, null, GetOptions.NONE, PendChangesOptions.NONE);
            int failures = checkinPendingChanges(workspace, "checking in file " + filePath + " to TFS");
            System.out.println(String.format("Failures checking-in: %d", failures));
        }else {
            System.out.println(String.format("Could not find workspace to check-in %s to.", filePath));
        }
    }
}
