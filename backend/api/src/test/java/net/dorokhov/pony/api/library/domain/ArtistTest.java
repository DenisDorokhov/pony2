package net.dorokhov.pony.api.library.domain;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtistTest {

    @Test
    public void shouldSort() {
        
        Artist artist1 = Artist.builder()
                .id("1")
                .name("1")
                .build();
        Artist artist2 = Artist.builder()
                .id("2")
                .name("2")
                .build();
        Artist artistNull = Artist.builder()
                .id("null")
                .build();
        
        Artist[] list = {artistNull, artist2, artist1, artist2};
        Arrays.sort(list);
        
        assertThat(list).containsExactly(artist1, artist2, artist2, artistNull);
    }

    @Test
    public void shouldSupportEqualityAndHashCode() {
        
        Artist eqArtist1 = Artist.builder().id("1").build();
        Artist eqArtist2 = Artist.builder().id("1").build();
        Artist diffArtist = Artist.builder().id("2").build();
        
        assertThat(eqArtist1.hashCode()).isEqualTo(eqArtist2.hashCode());
        assertThat(eqArtist1.hashCode()).isNotEqualTo(diffArtist.hashCode());

        assertThat(eqArtist1).isEqualTo(eqArtist1);
        assertThat(eqArtist1).isEqualTo(eqArtist2);

        assertThat(eqArtist1).isNotEqualTo(diffArtist);
        assertThat(eqArtist1).isNotEqualTo("foo1");
        assertThat(eqArtist1).isNotEqualTo(null);
    }
}
