package net.dorokhov.pony.library.service.internal.artwork.command;

import com.google.common.io.ByteSource;

import static com.google.common.base.Preconditions.checkNotNull;

public class ByteSourceArtworkDraft extends ArtworkDraft {
    
    private final ByteSource byteSource;

    public ByteSourceArtworkDraft(ByteSource byteSource, String sourceUri) {
        super(sourceUri);
        this.byteSource = checkNotNull(byteSource);
    }

    public ByteSource getByteSource() {
        return byteSource;
    }
}
