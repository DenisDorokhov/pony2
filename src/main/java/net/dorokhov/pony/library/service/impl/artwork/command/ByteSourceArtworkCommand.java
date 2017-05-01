package net.dorokhov.pony.library.service.impl.artwork.command;

import com.google.common.io.ByteSource;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ByteSourceArtworkCommand {

    private final String sourceUri;
    
    private final ByteSource byteSource;

    public ByteSourceArtworkCommand(String sourceUri, ByteSource byteSource) {
        this.sourceUri = checkNotNull(sourceUri);
        this.byteSource = checkNotNull(byteSource);
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public ByteSource getByteSource() {
        return byteSource;
    }
}
