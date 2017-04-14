package net.dorokhov.pony.artwork;

import com.google.common.io.ByteSource;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SaveByteSourceArtworkCommand extends SaveArtworkCommand {
    
    private final ByteSource byteSource;

    public SaveByteSourceArtworkCommand(ByteSource byteSource, String tag) {
        this(byteSource, tag, null);
    }

    public SaveByteSourceArtworkCommand(ByteSource byteSource, String tag, Map<String, String> metaData) {
        super(tag, metaData);
        checkNotNull(byteSource);
        this.byteSource = byteSource;
    }

    public ByteSource getByteSource() {
        return byteSource;
    }
}
