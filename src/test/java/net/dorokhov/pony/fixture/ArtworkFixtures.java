package net.dorokhov.pony.fixture;

import net.dorokhov.pony.library.domain.Artwork;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

public final class ArtworkFixtures {

    private ArtworkFixtures() {
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
                        .fromUriString("sourceUri")
                        .build().toUri());
    }
}
