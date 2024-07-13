package net.dorokhov.pony3.core.library.service.artwork.command;

import com.google.common.io.ByteSource;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ByteSourceArtworkStorageCommand {

    private final URI sourceUri;
    
    private final ByteSource byteSource;

    public ByteSourceArtworkStorageCommand(URI sourceUri, ByteSource byteSource) {
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
