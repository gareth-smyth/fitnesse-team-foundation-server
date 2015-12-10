package fitnesse.wiki.fs;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import fitnesse.wiki.VersionInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static fitnesse.wiki.fs.TfsPropertiesHelper.getNativeTfsFolder;
import static fitnesse.wiki.fs.TfsPropertiesHelper.getTfsServer;

public class TfsFileVersionController implements VersionsController {
    public static final String NATIVE_FOLDER_SYSTEM_PROPERTY_KEY = "com.microsoft.tfs.jni.native.base-directory";
    private final VersionsController persistence;
    private final TfsWrapper tfsWrapper;

    public TfsFileVersionController(Properties properties) {
        System.setProperty(NATIVE_FOLDER_SYSTEM_PROPERTY_KEY, getNativeTfsFolder(properties));
        tfsWrapper = new TfsWrapper(getTfsServer(properties));
        persistence = new SimpleFileVersionsController(new DiskFileSystem());
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
        ArrayList<VersionInfo> history = new ArrayList<VersionInfo>();
        for (File file : files) {
            List<Changeset> changesets = tfsWrapper.getHistory(file);
            for (Changeset changeset : changesets) {
                history.add(new VersionInfo(changeset.getComment(), changeset.getOwner(), changeset.getDate().getTime()));
            }
        }
        return history;
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
    public void delete(FileVersion... fileVersions) {
        for (FileVersion fileVersion : fileVersions) {
            tfsWrapper.delete(fileVersion.getFile());
        }
    }
}
