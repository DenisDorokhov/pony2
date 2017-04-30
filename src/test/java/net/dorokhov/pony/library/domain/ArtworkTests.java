package net.dorokhov.pony.library.domain;

import net.dorokhov.pony.library.domain.Artwork;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtworkTests {

    @Test
    public void supportEqualityAndHashCode() throws Exception {

        Artwork eqArtwork1 = buildArtwork().id(1L).build();
        Artwork eqArtwork2 = buildArtwork().id(1L).build();
        Artwork diffArtwork = buildArtwork().id(2L).build();

        assertThat(eqArtwork1.hashCode()).isEqualTo(eqArtwork2.hashCode());
        assertThat(eqArtwork1.hashCode()).isNotEqualTo(diffArtwork.hashCode());

        assertThat(eqArtwork1).isEqualTo(eqArtwork1);
        assertThat(eqArtwork1).isEqualTo(eqArtwork2);

        assertThat(eqArtwork1).isNotEqualTo(diffArtwork);
        assertThat(eqArtwork1).isNotEqualTo("foo1");
        assertThat(eqArtwork1).isNotEqualTo(null);
    }

    @Test
    public void stringify() throws Exception {
        assertThat(new Artwork().toString()).startsWith("Artwork{");
    }
    
    private Artwork.Builder buildArtwork() {
        return Artwork.builder()
                .mimeType("image/png")
                .checksum("someChecksum")
                .largeImageSize(0L)
                .largeImagePath("someLargePath")
                .smallImageSize(0L)
                .smallImagePath("someSmallPath")
                .sourceUri("sourceUri");
    }
}
