package net.dorokhov.pony2.web.dto;

import java.io.File;
import java.time.LocalDateTime;

public final class FileDistribution {

    private File file;
    private String name;
    private String mimeType;
    private LocalDateTime modificationDate;

    public File getFile() {
        return file;
    }

    public FileDistribution setFile(File file) {
        this.file = file;
        return this;
    }

    public String getName() {
        return name;
    }

    public FileDistribution setName(String name) {
        this.name = name;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileDistribution setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public LocalDateTime getModificationDate() {
        return modificationDate;
    }

    public FileDistribution setModificationDate(LocalDateTime modificationDate) {
        this.modificationDate = modificationDate;
        return this;
    }
}
