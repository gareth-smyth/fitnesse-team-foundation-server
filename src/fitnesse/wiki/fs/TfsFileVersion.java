package fitnesse.wiki.fs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class TfsFileVersion implements FileVersion {
    private final File file;
    private final byte[] content;
    private final String author;
    private final Date lastModified;

    public TfsFileVersion(File file, byte[] content, String author, Date modified) {
        this.file = file;
        this.content = content;
        this.author = author;
        this.lastModified = modified;
    }

    @Override
    public File getFile() {
        System.out.println("Getting file:"+file.exists());
        return file;
    }

    @Override
    public InputStream getContent() throws IOException {
        System.out.println("Getting content:"+content.length);
        return new ByteArrayInputStream(content);
    }

    @Override
    public String getAuthor() {
        System.out.println("Getting author:"+author);
        return author;
    }

    @Override
    public Date getLastModificationTime() {
        System.out.println("Getting time:"+lastModified);
        return lastModified;
    }
}
