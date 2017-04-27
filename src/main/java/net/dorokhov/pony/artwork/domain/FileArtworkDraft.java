package net.dorokhov.pony.artwork.domain;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public class FileArtworkDraft extends ArtworkDraft {

    private final File file;

    public FileArtworkDraft(File file, String sourceUri) {
        super(sourceUri);
        this.file = checkNotNull(file);
    }

    public File getFile() {
        return file;
    }
}
