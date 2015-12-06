package fitnesse.wiki.fs;

import java.io.File;
import java.util.Properties;

public class TfsPropertiesHelper {
    public static final String NATIVE_FOLDER_PROPERTY_KEY = "TfsVersionsController.NativeLibFolder";
    public static final String TFS_SERVER_PROPERTY_KEY = "TfsVersionsController.TfsServerUri";

    public static String getNativeTfsFolder(Properties properties) {
        final String nativeTfsFolder = properties.getProperty(NATIVE_FOLDER_PROPERTY_KEY);
        if(nativeTfsFolder==null)
            throw new TfsFileVersionControllerException("plugins.properties MUST have the TfsVersionsController.NativeLibFolder property set.");
        File libraryDirectory = new File(nativeTfsFolder);
        if(!libraryDirectory.exists()) {
            StringBuilder message = new StringBuilder("Could not find TfsVersionsController.NativeLibFolder.\n");
            message.append("Current folder "+System.getProperty("user.dir")+"\n");
            message.append("Native folder supplied "+libraryDirectory+"\n");
            throw new TfsFileVersionControllerException(message.toString());
        }
        return nativeTfsFolder;
    }

    public static String getTfsServer(Properties properties) {
        final String tfsServerUri = properties.getProperty(TFS_SERVER_PROPERTY_KEY);
        if(tfsServerUri==null)
            throw new TfsFileVersionControllerException("plugins.properties MUST have the TfsVersionsController.TfsServerUri property set.");
        return tfsServerUri;
    }
}
