package fitnesse.wiki.fs;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.AutoResolveOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.util.URIUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

public class TfsHelper {
    private Workspace workspace;
    private VersionControlClient versionControlClient;
    private PersistenceStoreProvider persistenceStoreProvider;

    public void createAndMapWorkspace(final String tfsServerUri,
                                      final String workspaceName,
                                      final String localPath,
                                      final String serverPath) {
        TFSTeamProjectCollection tfsTeamProjectCollection =
                new TFSTeamProjectCollection(URIUtils.newURI(tfsServerUri), new DefaultNTCredentials());

        versionControlClient = tfsTeamProjectCollection.getVersionControlClient();
        workspace = versionControlClient.tryGetWorkspace(localPath);

        if (workspace != null) {
            versionControlClient.deleteWorkspace(workspace);
            System.out.println("Local path '" + localPath + "' is already mapped to workspace '" + workspace.getName() + "'. Deleted.");
        }

        workspace =
                versionControlClient.createWorkspace(
                        null,
                        workspaceName,
                        "",
                        WorkspaceLocation.LOCAL,
                        null);

        WorkingFolder workingFolder = new WorkingFolder(serverPath, LocalPath.canonicalize(localPath));
        workspace.createWorkingFolder(workingFolder);

        System.out.println("Workspace '" + workspaceName + "' now exists and is mapped to path '" + localPath + "'");
    }

    public void cleanUp() {
        versionControlClient.deleteWorkspace(workspace);
    }
}
