package net.dorokhov.pony3.api.library.domain;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ArtworkFiles {
    
    private final Artwork artwork;
    private final File smallFile;
    private final File largeFile;

    public ArtworkFiles(Artwork artwork, File smallFile, File largeFile) {
        this.artwork = checkNotNull(artwork);
        this.smallFile = checkNotNull(smallFile);
        this.largeFile = checkNotNull(largeFile);
    }

    public Artwork getArtwork() {
        return artwork;
    }

    public File getSmallFile() {
        return smallFile;
    }

    public File getLargeFile() {
        return largeFile;
    }
}
