package net.dorokhov.pony.library.service.internal.artwork.command;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public class FileArtworkCommand extends ArtworkCommand {

    private final File file;

    public FileArtworkCommand(File file, String sourceUri) {
        super(sourceUri);
        this.file = checkNotNull(file);
    }

    public File getFile() {
        return file;
    }
}
