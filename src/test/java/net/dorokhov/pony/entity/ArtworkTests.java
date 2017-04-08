package net.dorokhov.pony.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtworkTests {

    @Test
    public void supportEqualityAndHashCode() throws Exception {

        Artwork eqArtwork1 = new Artwork();
        eqArtwork1.setId(1L);
        Artwork eqArtwork2 = new Artwork();
        eqArtwork2.setId(1L);
        Artwork diffArtwork = new Artwork();
        diffArtwork.setId(2L);

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
}
