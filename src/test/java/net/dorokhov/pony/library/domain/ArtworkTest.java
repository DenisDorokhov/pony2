package net.dorokhov.pony.library.domain;

import org.junit.Test;

import static net.dorokhov.pony.fixture.ArtworkFixtures.artworkBuilder;
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
}
