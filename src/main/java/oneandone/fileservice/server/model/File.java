package oneandone.fileservice.server.model;

import java.util.Date;

public class File {

    /**
     * Name of the file together with the extension
     */
    private String name;
    /**
     * Actual content of the file
     */
    private byte[] content;
    /**
     * Last modified date used in concurrency control.
     */
    private long lastModified;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "File{" +
                "name='" + name + '\'' +
                '}';
    }
}
