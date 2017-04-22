package net.dorokhov.pony.artwork.draft;

import com.google.common.io.ByteSource;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ByteSourceArtworkDraft extends ArtworkDraft {
    
    private final ByteSource byteSource;

    public ByteSourceArtworkDraft(ByteSource byteSource, String tag) {
        this(byteSource, tag, null);
    }

    public ByteSourceArtworkDraft(ByteSource byteSource, String tag, Map<String, String> metaData) {
        super(tag, metaData);
        checkNotNull(byteSource);
        this.byteSource = byteSource;
    }

    public ByteSource getByteSource() {
        return byteSource;
    }
}
