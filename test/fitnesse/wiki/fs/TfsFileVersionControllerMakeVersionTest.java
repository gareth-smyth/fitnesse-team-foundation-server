package fitnesse.wiki.fs;

import fitnesse.ConfigurationParameter;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TfsFileVersionControllerMakeVersionTest {
    public static final String NATIVE_FOLDER_PROPERTY_VALUE = "team-foundation-server-sdk-redist/native";
    public static final String TFS_SERVER_PROPERTY_VALUE = "http://localhost:8080/tfs/DefaultCollection/";
    public static final String TEST_FILES_FOLDER = "testWikiRoot"+System.currentTimeMillis();
    public static final String TFS_PROJECT = "$/FitnesseTfsTest";
    private TfsFileVersionController tfsFileVersionController;
    private TfsHelper tfsHelper;

    @Before
    public void initialise() throws IOException {
        final Properties properties = new Properties();
        properties.setProperty(ConfigurationParameter.VERSIONS_CONTROLLER_CLASS.getKey(), TfsFileVersionController.class.getCanonicalName());
        properties.setProperty(TfsPropertiesHelper.NATIVE_FOLDER_PROPERTY_KEY, NATIVE_FOLDER_PROPERTY_VALUE);
        properties.setProperty(TfsPropertiesHelper.TFS_SERVER_PROPERTY_KEY, TFS_SERVER_PROPERTY_VALUE);
        tfsFileVersionController = new TfsFileVersionController(properties);

        tfsHelper = new TfsHelper();
        tfsHelper.createAndMapWorkspace(TFS_SERVER_PROPERTY_VALUE,
                                        "fitnessTfsTest"+System.currentTimeMillis(),
                                        System.getProperty("user.dir")+"\\"+TEST_FILES_FOLDER+"\\RooT\\",
                                        TFS_PROJECT+"/"+TEST_FILES_FOLDER);
    }

    @After
    public void destroy(){
        tfsHelper.cleanUp();
        FileUtil.deleteFileSystemDirectory(TEST_FILES_FOLDER);
    }

    @Test
    public void shouldSaveGivenFilesToDisk() throws IOException {
        // Setup
        final FileVersion fileVersion1 = makeFileVersion("filename1", "File one content");
        final FileVersion fileVersion2 = makeFileVersion("filename2", "File two content");

        // Execute
        tfsFileVersionController.makeVersion(fileVersion1, fileVersion2);

        // Assert
        List<String> file1Content = Files.readAllLines(fileVersion1.getFile().toPath(), StandardCharsets.UTF_8);
        List<String> file2Content = Files.readAllLines(fileVersion2.getFile().toPath(), StandardCharsets.UTF_8);
        assertThat(file1Content, contains("File one content"));
        assertThat(file2Content, contains("File two content"));

        // Clean up
        tfsFileVersionController.delete(fileVersion1, fileVersion2);
    }

    @Test
    public void shouldAddGivenFilesToTfs() throws IOException {
        // Setup
        final FileVersion fileVersion1 = makeFileVersion("filename1", "File one content");
        final FileVersion fileVersion2 = makeFileVersion("filename2", "File two content");

        // Execute
        tfsFileVersionController.makeVersion(fileVersion1, fileVersion2);

        // Cheat
        Boolean cheatWorked = cheatDelete(fileVersion1, fileVersion2);
        assertTrue("Trying to delete files created on disk to check they are retrieved from TFS failed.  This is a failure of how the test works.", cheatWorked);

        // Assert
        FileVersion[] revisionData = tfsFileVersionController.getRevisionData(null, fileVersion1.getFile(), fileVersion2.getFile());

        List<String> file1Content = IOUtils.readLines(revisionData[0].getContent(), StandardCharsets.UTF_8);
        List<String> file2Content = IOUtils.readLines(revisionData[1].getContent(), StandardCharsets.UTF_8);
        assertThat(file1Content, contains("File one content"));
        assertThat(file2Content, contains("File two content"));

        // Clean up local files
        tfsFileVersionController.delete(fileVersion1, fileVersion2);
    }

    private Boolean cheatDelete(FileVersion... fileVersions) {
        Boolean cheatWorked = true;
        for (FileVersion fileVersion : fileVersions) {
            cheatWorked = cheatWorked && fileVersion.getFile().delete();
        }
        return cheatWorked;
    }

    @Test
    public void shouldAddNewVersionOfGivenFilesToTfs() throws IOException {
        // Setup
        final FileVersion fileVersion1 = makeFileVersion("filename1", "File one content");
        final FileVersion fileVersion2 = makeFileVersion("filename2", "File two content");
        final FileVersion fileVersion1b = makeFileVersion("filename1", "File one content changed");
        final FileVersion fileVersion2b = makeFileVersion("filename2", "File two content changed");

        // Execute
        tfsFileVersionController.makeVersion(fileVersion1, fileVersion2);
        tfsFileVersionController.makeVersion(fileVersion1b, fileVersion2b);

        // Cheat
        Boolean cheatWorked = cheatDelete(fileVersion1b, fileVersion2b);
        assertTrue("Trying to delete files created on disk to check they are retrieved from TFS failed.  This is a failure of how the test works.", cheatWorked);

        // Assert
        @SuppressWarnings("unchecked")
        List<FileVersion> revisionData = new ArrayList(tfsFileVersionController.history(fileVersion1b.getFile(), fileVersion2b.getFile()));

        assertThat(revisionData.get(0), hasProperty("name", containsString("filename1")));
        assertThat(revisionData.get(1), hasProperty("name", containsString("filename1")));
        assertThat(revisionData.get(2), hasProperty("name", containsString("filename2")));
        assertThat(revisionData.get(3), hasProperty("name", containsString("filename2")));

        // Clean up local files
        tfsFileVersionController.delete(fileVersion1, fileVersion2, fileVersion1b, fileVersion2b);
    }

    private FileVersion makeFileVersion(final String fileName, final String content) throws IOException {
        final File file = new File(TEST_FILES_FOLDER +"/RooT/"+fileName);
        byte[] contentAsBytes = content.getBytes(StandardCharsets.UTF_8);
        return new TfsFileVersion(file, contentAsBytes, "", new Date());
    }
}
