package fitnesse.wiki.fs;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkspaceItem;
import fitnesse.wiki.VersionInfo;
import org.hsqldb.lib.StringInputStream;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TfsFileVersionControllerTest {
    private final TfsWrapper mockTfsWrapper = mock(TfsWrapper.class);
    private final VersionsController baseFileVersionsController = mock(VersionsController.class);
    private final TfsFileVersionController tfsFileVersionController = new TfsFileVersionController("nativeFolder", mockTfsWrapper, baseFileVersionsController);;

    private final Workspace[] repositoryWorkspaces = new Workspace[1];

    @Before
    public void initialise() throws IOException {
        when(mockTfsWrapper.getRepositoryWorkspaces()).thenReturn(repositoryWorkspaces);
    }

    @Test
    public void shouldGetExistingFileFromTfs() throws IOException {
        final File testExistingFileInTfs = new File("existingDownloadedFilePath.txt");

        Files.write(testExistingFileInTfs.toPath(), "Tfs existing file text".getBytes(StandardCharsets.UTF_8));
        WorkspaceItem mockExistingWorkspaceItem = mock(WorkspaceItem.class);

        when(mockTfsWrapper.getRepositoryFile(mockExistingWorkspaceItem, testExistingFileInTfs.getName())).thenReturn(testExistingFileInTfs);
        when(mockTfsWrapper.getWorkspaceItem(testExistingFileInTfs.getAbsolutePath())).thenReturn(mockExistingWorkspaceItem);

        FileVersion[] revisionData = tfsFileVersionController.getRevisionData(null, testExistingFileInTfs);

        assertThat(revisionData.length, is(1));
        List<String> fileContent = org.apache.commons.io.IOUtils.readLines(revisionData[0].getContent());
        assertThat(fileContent, hasItems("Tfs existing file text"));
    }

    @Test
    public void shouldGetLocalFileWhenUnknownInTfs() throws IOException {
        final File testExistingLocalFile = new File("existingLocalFilePath.txt");
        Files.write(testExistingLocalFile.toPath(), "Local existing file text".getBytes(StandardCharsets.UTF_8));

        when(mockTfsWrapper.getWorkspaceItem(testExistingLocalFile.getAbsolutePath())).thenReturn(null);
        when(baseFileVersionsController.getRevisionData(null, testExistingLocalFile)).thenReturn(new FileVersion[] { new TfsFileVersion(testExistingLocalFile, "Local existing file text".getBytes(StandardCharsets.UTF_8),"", null)});

        FileVersion[] revisionData = tfsFileVersionController.getRevisionData(null, testExistingLocalFile);

        assertThat(revisionData.length, is(1));
        List<String> fileContent = org.apache.commons.io.IOUtils.readLines(revisionData[0].getContent());
        assertThat(fileContent, hasItems("Local existing file text"));
    }

    @Test
    public void shouldAddFileToTfsIfItDoesntExist() throws IOException {
        final File testNonExistingLocalFile = new File("existingLocalFilePath.txt");
        Files.write(testNonExistingLocalFile.toPath(), "Local non-existing file text".getBytes(StandardCharsets.UTF_8));

        FileVersion mockLocalFileVersion = mock(FileVersion.class);
        when(mockLocalFileVersion.getAuthor()).thenReturn("");
        when(mockLocalFileVersion.getContent()).thenReturn(new BufferedInputStream(new StringInputStream("Local non-existing file text")));
        when(mockLocalFileVersion.getFile()).thenReturn(testNonExistingLocalFile);
        when(mockLocalFileVersion.getLastModificationTime()).thenReturn(null);


        VersionInfo versionInfo = tfsFileVersionController.makeVersion(mockLocalFileVersion);
        verify(baseFileVersionsController, times(1)).makeVersion(mockLocalFileVersion);
    }
}
