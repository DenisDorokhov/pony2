package net.dorokhov.pony2.api.library.domain;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtworkTest {

    @Test
    public void shouldSupportEqualityAndHashCode() {

        Artwork eqArtwork1 = artwork().setId("1");
        Artwork eqArtwork2 = artwork().setId("1");
        Artwork diffArtwork = artwork().setId("2");

        assertThat(eqArtwork1.hashCode()).isEqualTo(eqArtwork2.hashCode());
        assertThat(eqArtwork1.hashCode()).isNotEqualTo(diffArtwork.hashCode());

        //noinspection EqualsWithItself
        assertThat(eqArtwork1).isEqualTo(eqArtwork1);
        assertThat(eqArtwork1).isEqualTo(eqArtwork2);

        assertThat(eqArtwork1).isNotEqualTo(diffArtwork);
        //noinspection AssertBetweenInconvertibleTypes
        assertThat(eqArtwork1).isNotEqualTo("foo1");
        assertThat(eqArtwork1).isNotEqualTo(null);
    }

    private Artwork artwork() {
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
