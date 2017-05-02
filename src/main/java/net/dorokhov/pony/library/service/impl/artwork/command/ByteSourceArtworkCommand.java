package net.dorokhov.pony.library.service.impl.artwork.command;

import com.google.common.io.ByteSource;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ByteSourceArtworkCommand {

    private final URI sourceUri;
    
    private final ByteSource byteSource;

    public ByteSourceArtworkCommand(URI sourceUri, ByteSource byteSource) {
        this.sourceUri = checkNotNull(sourceUri);
        this.byteSource = checkNotNull(byteSource);
    }

    public URI getSourceUri() {
        return sourceUri;
    }

    public ByteSource getByteSource() {
        return byteSource;
    }
}
