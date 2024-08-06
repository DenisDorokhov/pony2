package net.dorokhov.pony2.test;

import net.dorokhov.pony2.api.library.domain.Artwork;
import net.dorokhov.pony2.api.library.domain.ArtworkFiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.time.LocalDateTime;

public final class ArtworkFixtures {

    private ArtworkFixtures() {
    }
    
    public static ArtworkFiles artworkFiles() {
        return artworkFiles(artwork());
    }
    
    public static ArtworkFiles artworkFiles(Artwork artwork) {
        return new ArtworkFiles(artwork, new File("smallFile"), new File("largeFile"));
    }

    public static Artwork artwork() {
        return new Artwork()
                .setId("1")
                .setDate(LocalDateTime.now())
                .setMimeType("image/png")
                .setChecksum("someChecksum")
                .setLargeImageSize(0L)
                .setLargeImagePath("someLargePath")
                .setSmallImageSize(0L)
                .setSmallImagePath("someSmallPath")
                .setSourceUri(UriComponentsBuilder
                        .fromUriString("file:sourceUri")
                        .build().toUri());
    }
}
