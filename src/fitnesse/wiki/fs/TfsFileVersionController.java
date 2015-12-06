package fitnesse.wiki.fs;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.*;
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
        for (File file : files) {
            String filePath = file.toPath().toAbsolutePath().normalize().toString();
            WorkspaceItem workspaceItem = tfsWrapper.getWorkspaceItem(filePath);

            if(workspaceItem!= null){
                System.out.println(String.format("Found item %s for file %s.", workspaceItem.getDownloadURL(), filePath));
                File tempFile = tfsWrapper.getRepositoryFile(workspaceItem, file.getName());
                try {
                    System.out.println(Files.readAllBytes(tempFile.toPath()).length);
                    fileVersions[fileCounter] = new TfsFileVersion(tempFile, Files.readAllBytes(tempFile.toPath()), "someone", new Date());
                } catch (IOException e) {
                    System.out.println("Whoops!  Saved the file from TFS to a temp location then couldn't open it or some such.");
                    e.printStackTrace();
                }
            }else{
                System.out.println(String.format("Could not find an item for %s in TFS, getting local.", filePath));
                fileVersions[fileCounter] =  persistence.getRevisionData(null, file)[0];
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
        for(FileVersion fileVersion : fileVersions){
            System.out.println("Making version:"+fileVersion.getFile().getAbsolutePath()+" "+fileVersion.getAuthor()+" "+fileVersion.getLastModificationTime());

            String filePath = fileVersion.getFile().toPath().toAbsolutePath().normalize().toString();
            System.out.println("File " + (fileVersion.getFile().exists() ? "EXISTS" : "DOES NOT EXIST"));
            WorkspaceItem workspaceItem = tfsWrapper.getWorkspaceItem(filePath);

            if(workspaceItem!= null){
                System.out.println(String.format("Found item for %s in TFS, checking in.", filePath));
                tfsWrapper.checkinPending(filePath);
            }else{
                System.out.println(String.format("Could not find an item for %s in TFS, adding.", filePath));
                tfsWrapper.addToRepository(filePath);
            }
        }

        return new VersionInfo("", "", new Date());
    }

    @Override
    public VersionInfo addDirectory(FileVersion filePath) throws IOException {
        System.out.println("Add directory:"+filePath.getFile().getAbsolutePath());
        return null;
    }

    @Override
    public void rename(FileVersion fileVersion, File originalFile) throws IOException {
        System.out.println("Rename file from:"+originalFile.getAbsolutePath()+" to "+fileVersion.getFile().getAbsolutePath());
    }

    @Override
    public void delete(FileVersion... files) {
        for(FileVersion file : files) {
            System.out.println("Delete files:" + file.getFile().getAbsolutePath());
        }
    }
}
