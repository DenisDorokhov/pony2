package net.dorokhov.pony2.api.library.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtistTest {

    @Test
    public void shouldSort() {
        
        Artist artist1 = new Artist()
                .setId("1")
                .setName("1");
        Artist artist2 = new Artist()
                .setId("2")
                .setName("2");
        Artist artistNull = new Artist()
                .setId("null");
        
        Artist[] list = {artistNull, artist2, artist1, artist2};
        Arrays.sort(list);
        
        assertThat(list).containsExactly(artist1, artist2, artist2, artistNull);
    }

    @Test
    public void shouldSupportEqualityAndHashCode() {
        
        Artist eqArtist1 = new Artist().setId("1");
        Artist eqArtist2 = new Artist().setId("1");
        Artist diffArtist = new Artist().setId("2");
        
        assertThat(eqArtist1.hashCode()).isEqualTo(eqArtist2.hashCode());
        assertThat(eqArtist1.hashCode()).isNotEqualTo(diffArtist.hashCode());

        assertThat(eqArtist1).isEqualTo(eqArtist1);
        assertThat(eqArtist1).isEqualTo(eqArtist2);

        assertThat(eqArtist1).isNotEqualTo(diffArtist);
        assertThat(eqArtist1).isNotEqualTo("foo1");
        assertThat(eqArtist1).isNotEqualTo(null);
    }
}
