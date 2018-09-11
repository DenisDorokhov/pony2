package net.dorokhov.pony.api.library.domain;

import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtworkTest {

    @Test
    public void shouldSupportEqualityAndHashCode() {

        Artwork eqArtwork1 = artworkBuilder().id("1").build();
        Artwork eqArtwork2 = artworkBuilder().id("1").build();
        Artwork diffArtwork = artworkBuilder().id("2").build();

        assertThat(eqArtwork1.hashCode()).isEqualTo(eqArtwork2.hashCode());
        assertThat(eqArtwork1.hashCode()).isNotEqualTo(diffArtwork.hashCode());

        assertThat(eqArtwork1).isEqualTo(eqArtwork1);
        assertThat(eqArtwork1).isEqualTo(eqArtwork2);

        assertThat(eqArtwork1).isNotEqualTo(diffArtwork);
        assertThat(eqArtwork1).isNotEqualTo("foo1");
        assertThat(eqArtwork1).isNotEqualTo(null);
    }

    private Artwork.Builder artworkBuilder() {
        return Artwork.builder()
                .id("1")
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
