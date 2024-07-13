package net.dorokhov.pony3.api.library.domain;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtworkTest {

    @Test
    public void shouldSupportEqualityAndHashCode() {

        Artwork eqArtwork1 = artworkBuilder().setId("1");
        Artwork eqArtwork2 = artworkBuilder().setId("1");
        Artwork diffArtwork = artworkBuilder().setId("2");

        assertThat(eqArtwork1.hashCode()).isEqualTo(eqArtwork2.hashCode());
        assertThat(eqArtwork1.hashCode()).isNotEqualTo(diffArtwork.hashCode());

        assertThat(eqArtwork1).isEqualTo(eqArtwork1);
        assertThat(eqArtwork1).isEqualTo(eqArtwork2);

        assertThat(eqArtwork1).isNotEqualTo(diffArtwork);
        assertThat(eqArtwork1).isNotEqualTo("foo1");
        assertThat(eqArtwork1).isNotEqualTo(null);
    }

    private Artwork artworkBuilder() {
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
