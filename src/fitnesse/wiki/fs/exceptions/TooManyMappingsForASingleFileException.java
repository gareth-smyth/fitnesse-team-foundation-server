package fitnesse.wiki.fs.exceptions;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkspaceItem;

public class TooManyMappingsForASingleFileException extends RuntimeException {
    public TooManyMappingsForASingleFileException(WorkspaceItem firstItem, WorkspaceItem secondItem, String mappedServerPath) {
        super(String.format("Found a two workspace items (%s, %s) for %s", firstItem.getDownloadURL(), secondItem.getDownloadURL(), mappedServerPath));
    }
}
