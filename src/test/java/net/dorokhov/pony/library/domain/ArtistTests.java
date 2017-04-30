package net.dorokhov.pony.library.domain;

import net.dorokhov.pony.library.domain.Artist;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtistTests {

    @Test
    public void sort() throws Exception {
        
        Artist artist1 = Artist.builder().name("1").build();
        Artist artist2 = Artist.builder().name("2").build();
        Artist artistNull = new Artist();
        
        Artist[] list = {artistNull, artist2, artist1, artist2};
        Arrays.sort(list);
        
        assertThat(list).containsExactly(artist1, artist2, artist2, artistNull);
    }

    @Test
    public void supportEqualityAndHashCode() throws Exception {
        
        Artist eqArtist1 = Artist.builder().id(1L).build();
        Artist eqArtist2 = Artist.builder().id(1L).build();
        Artist diffArtist = Artist.builder().id(2L).build();
        
        assertThat(eqArtist1.hashCode()).isEqualTo(eqArtist2.hashCode());
        assertThat(eqArtist1.hashCode()).isNotEqualTo(diffArtist.hashCode());

        assertThat(eqArtist1).isEqualTo(eqArtist1);
        assertThat(eqArtist1).isEqualTo(eqArtist2);

        assertThat(eqArtist1).isNotEqualTo(diffArtist);
        assertThat(eqArtist1).isNotEqualTo("foo1");
        assertThat(eqArtist1).isNotEqualTo(null);
    }

    @Test
    public void stringify() throws Exception {
        assertThat(new Artist().toString()).startsWith("Artist{");
    }
}
