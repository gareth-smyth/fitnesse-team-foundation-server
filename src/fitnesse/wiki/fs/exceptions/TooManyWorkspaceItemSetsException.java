package fitnesse.wiki.fs.exceptions;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkspaceItemSet;

public class TooManyWorkspaceItemSetsException extends RuntimeException {
    public TooManyWorkspaceItemSetsException(WorkspaceItemSet[] workspaceItemSets) {
        super(createMessage(workspaceItemSets));
    }

    private static String createMessage(WorkspaceItemSet[] workspaceItemSets) {
        String message = "Found the server path mapping, but calling getItems returned the following items:";
        for(WorkspaceItemSet workspaceItemSet : workspaceItemSets){
            message += workspaceItemSet.getQueryPath()+" ";
        }
        return message;
    }
}
