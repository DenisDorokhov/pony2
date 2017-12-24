package net.dorokhov.pony.fixture;

import java.io.File;
import java.time.LocalDateTime;

import net.dorokhov.pony.api.library.domain.Artwork;
import net.dorokhov.pony.api.library.domain.ArtworkFiles;
import org.springframework.web.util.UriComponentsBuilder;

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
        return artworkBuilder().build();
    }

    public static Artwork.Builder artworkBuilder() {
        return Artwork.builder()
                .id(1L)
                .date(LocalDateTime.now())
                .mimeType("image/png")
                .checksum("someChecksum")
                .largeImageSize(0L)
                .largeImagePath("someLargePath")
                .smallImageSize(0L)
                .smallImagePath("someSmallPath")
                .sourceUri(UriComponentsBuilder
                        .fromUriString("file:sourceUri")
                        .build().toUri());
    }
}
