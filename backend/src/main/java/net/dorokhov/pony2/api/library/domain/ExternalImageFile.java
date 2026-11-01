package net.dorokhov.pony2.api.library.domain;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ExternalImageFile {

    private final ExternalImage externalImage;
    private final File file;

    public ExternalImageFile(ExternalImage externalImage, File file) {
        this.externalImage = checkNotNull(externalImage);
        this.file = checkNotNull(file);
    }

    public ExternalImage getExternalImage() {
        return externalImage;
    }

    public File getFile() {
        return file;
    }
}
