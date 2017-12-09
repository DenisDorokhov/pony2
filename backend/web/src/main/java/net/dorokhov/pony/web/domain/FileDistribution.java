package net.dorokhov.pony.web.domain;

import java.io.File;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FileDistribution {

    private final File file;
    private final String name;
    private final String mimeType;
    private final LocalDateTime modificationDate;

    public FileDistribution(File file, String name, String mimeType, LocalDateTime modificationDate) {
        this.file = checkNotNull(file);
        this.name = checkNotNull(name);
        this.mimeType = checkNotNull(mimeType);
        this.modificationDate = checkNotNull(modificationDate);
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public LocalDateTime getModificationDate() {
        return modificationDate;
    }
}
