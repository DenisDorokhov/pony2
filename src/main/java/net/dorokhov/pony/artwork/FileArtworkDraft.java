package net.dorokhov.pony.artwork;

import java.io.File;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class FileArtworkDraft extends ArtworkDraft {

    private final File file;

    public FileArtworkDraft(File file, String tag) {
        this(file, tag, null);
    }

    public FileArtworkDraft(File file, String tag, Map<String, String> metaData) {
        super(tag, metaData);
        checkNotNull(file);
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}
