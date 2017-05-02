package net.dorokhov.pony.library.service.impl.artwork.command;

import java.io.File;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FileArtworkCommand {
    
    private final URI sourceUri;

    private final File file;

    public FileArtworkCommand(URI sourceUri, File file) {
        this.sourceUri = checkNotNull(sourceUri);
        this.file = checkNotNull(file);
    }

    public URI getSourceUri() {
        return sourceUri;
    }

    public File getFile() {
        return file;
    }
}
