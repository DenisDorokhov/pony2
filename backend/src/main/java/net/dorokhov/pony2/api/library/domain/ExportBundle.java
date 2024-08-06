package net.dorokhov.pony2.api.library.domain;

import java.io.IOException;
import java.io.OutputStream;

public final class ExportBundle {
    
    public interface Content {
        void write(OutputStream outputStream) throws IOException;
    }
    
    private final String fileName;
    private final String mimeType;
    private final Content content;

    public ExportBundle(String fileName, String mimeType, Content content) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.content = content;
    }
    
    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Content getContent() {
        return content;
    }
}
