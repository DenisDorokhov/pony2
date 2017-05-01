package net.dorokhov.pony.library.service.impl.artwork.command;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public class FileArtworkCommand {
    
    private final String sourceUri;

    private final File file;

    public FileArtworkCommand(String sourceUri, File file) {
        this.sourceUri = checkNotNull(sourceUri);
        this.file = checkNotNull(file);
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public File getFile() {
        return file;
    }
}
