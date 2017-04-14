package net.dorokhov.pony.artwork;

import java.io.File;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SaveFileArtworkCommand extends SaveArtworkCommand {

    private final File file;

    public SaveFileArtworkCommand(File file, String tag) {
        this(file, tag, null);
    }

    public SaveFileArtworkCommand(File file, String tag, Map<String, String> metaData) {
        super(tag, metaData);
        checkNotNull(file);
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}
