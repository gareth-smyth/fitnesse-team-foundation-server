package fitnesse.wiki.fs.exceptions;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkspaceItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkspaceItemSet;

public class TooManyWorkspaceItemsException extends RuntimeException {
    public TooManyWorkspaceItemsException(WorkspaceItemSet workspaceItemSet, WorkspaceItem[] workspaceItems) {
        super(createMessage(workspaceItemSet, workspaceItems));
    }

    private static String createMessage(WorkspaceItemSet workspaceItemSet, WorkspaceItem[] workspaceItems) {
        String message = "Found the workspace item set, but calling getItems returned the following items:";
        for(WorkspaceItem workspaceItem : workspaceItems){
            message += workspaceItem.getDownloadURL()+" ";
        }
        return message;
    }
}
