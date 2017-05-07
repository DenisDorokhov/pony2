package net.dorokhov.pony.library.domain;

import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtworkTest {

    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

        Artwork eqArtwork1 = artworkBuilder().id(1L).build();
        Artwork eqArtwork2 = artworkBuilder().id(1L).build();
        Artwork diffArtwork = artworkBuilder().id(2L).build();

        assertThat(eqArtwork1.hashCode()).isEqualTo(eqArtwork2.hashCode());
        assertThat(eqArtwork1.hashCode()).isNotEqualTo(diffArtwork.hashCode());

        assertThat(eqArtwork1).isEqualTo(eqArtwork1);
        assertThat(eqArtwork1).isEqualTo(eqArtwork2);

        assertThat(eqArtwork1).isNotEqualTo(diffArtwork);
        assertThat(eqArtwork1).isNotEqualTo("foo1");
        assertThat(eqArtwork1).isNotEqualTo(null);
    }

    @Test
    public void shouldImplementToString() throws Exception {
        assertThat(new Artwork().toString()).startsWith("Artwork{");
    }
    
    private Artwork.Builder artworkBuilder() {
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
