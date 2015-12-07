package fitnesse.wiki.fs;

import fitnesse.wiki.VersionInfo;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import static fitnesse.wiki.fs.TfsPropertiesHelper.*;
import static fitnesse.wiki.fs.TfsPropertiesHelper.getNativeTfsFolder;

public class TfsFileVersionController implements VersionsController {
    public static final String NATIVE_FOLDER_SYSTEM_PROPERTY_KEY = "com.microsoft.tfs.jni.native.base-directory";
    private final VersionsController persistence;
    private final TfsWrapper tfsWrapper;

    protected TfsFileVersionController(String nativeTfsFolder, TfsWrapper tfsWrapper, VersionsController fileVersionsController) {
        System.setProperty(NATIVE_FOLDER_SYSTEM_PROPERTY_KEY, nativeTfsFolder);
        this.persistence = fileVersionsController;
        this.tfsWrapper = tfsWrapper;
    }

    public TfsFileVersionController(Properties properties) {
        this(getNativeTfsFolder(properties), new TfsWrapper(getTfsServer(properties)), new SimpleFileVersionsController(new DiskFileSystem()));
        tfsWrapper.initialise();
    }

    @Override
    public FileVersion[] getRevisionData(String revision, File... files) {
        FileVersion[] fileVersions = new FileVersion[files.length];

        int fileCounter = 0;
        for (File localFile : files) {

            File downloadedFile = tfsWrapper.getRepositoryFile(localFile);
            if (downloadedFile != null) {
                try {
                    fileVersions[fileCounter] = new TfsFileVersion(downloadedFile, Files.readAllBytes(downloadedFile.toPath()), null, new Date());
                } catch (IOException e) {
                    System.out.println("Whoops!  Saved the file from TFS to a temp location then couldn't open it or some such.");
                    e.printStackTrace();
                }
            } else {
                fileVersions[fileCounter] = persistence.getRevisionData(null, localFile)[0];
            }
            fileCounter++;
        }

        return fileVersions;
    }

    @Override
    public Collection<? extends VersionInfo> history(File... files) {
        return null;
    }

    @Override
    public VersionInfo makeVersion(FileVersion... fileVersions) throws IOException {
        persistence.makeVersion(fileVersions);
        for (FileVersion fileVersion : fileVersions) {
            File existingFile = tfsWrapper.getRepositoryFile(fileVersion.getFile());

            if (existingFile != null) {
                tfsWrapper.checkinPending(fileVersion.getFile());
            } else {
                tfsWrapper.addToRepository(fileVersion.getFile());
            }
        }

        return new VersionInfo("", "", new Date());
    }

    @Override
    public VersionInfo addDirectory(FileVersion filePath) throws IOException {
        System.out.println("Add directory:" + filePath.getFile().getAbsolutePath());
        return null;
    }

    @Override
    public void rename(FileVersion fileVersion, File originalFile) throws IOException {
        System.out.println("Rename file from:" + originalFile.getAbsolutePath() + " to " + fileVersion.getFile().getAbsolutePath());
    }

    @Override
    public void delete(FileVersion... files) {
        for (FileVersion file : files) {
            System.out.println("Delete files:" + file.getFile().getAbsolutePath());
        }
    }
}
