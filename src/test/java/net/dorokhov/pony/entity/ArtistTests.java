package net.dorokhov.pony.entity;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtistTests {

    @Test
    public void sort() throws Exception {
        
        Artist artist1 = new Artist();
        artist1.setName("1");
        Artist artist2 = new Artist();
        artist2.setName("2");
        Artist artistNull = new Artist();
        artistNull.setName(null);
        
        Artist[] list = {artistNull, artist2, artist1, artist2};
        Arrays.sort(list);
        
        assertThat(list).containsExactly(artist1, artist2, artist2, artistNull);
    }

    @Test
    public void buildSearchTerms() throws Exception {
        Artist artist = new Artist();
        artist.setName("Foo");
        assertThat(artist.getSearchTerms()).isEqualTo("Foo");
        artist.setName(null);
        assertThat(artist.getSearchTerms()).isEqualTo("");
    }

    @Test
    public void supportEqualityAndHashCode() throws Exception {
        
        Artist eqArtist1 = new Artist();
        eqArtist1.setId(1L);
        Artist eqArtist2 = new Artist();
        eqArtist2.setId(1L);
        Artist diffArtist = new Artist();
        diffArtist.setId(2L);
        
        assertThat(eqArtist1.hashCode()).isEqualTo(eqArtist2.hashCode());
        assertThat(eqArtist1.hashCode()).isNotEqualTo(diffArtist.hashCode());

        assertThat(eqArtist1).isEqualTo(eqArtist1);
        assertThat(eqArtist1).isEqualTo(eqArtist2);

        assertThat(eqArtist1).isNotEqualTo(diffArtist);
        assertThat(eqArtist1).isNotEqualTo("foo1");
        assertThat(eqArtist1).isNotEqualTo(null);
    }
}
