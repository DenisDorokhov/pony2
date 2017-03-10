package net.dorokhov.pony.entity;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AlbumTests {

    @Test
    public void shouldBeSorted() throws Exception {

        Artist artist1 = new Artist();
        artist1.setName("1");

        Artist artist2 = new Artist();
        artist2.setName("2");

        Album album1_1 = new Album(artist1);
        album1_1.setName("1_1");
        album1_1.setYear(1000);
        Album album1_2_1002 = new Album(artist1);
        album1_2_1002.setName("1_2");
        album1_2_1002.setYear(1002);
        Album album1_2_1001 = new Album(artist1);
        album1_2_1001.setName("1_2");
        album1_2_1001.setYear(1001);

        Album album2_1 = new Album(artist2);
        album2_1.setName("2_1");
        Album album2_null = new Album(artist2);
        album2_null.setName(null);

        Album[] list = {album2_null, album2_1, album1_2_1002, album1_2_1001, album1_1};
        Arrays.sort(list);

        assertThat(list).containsExactly(album1_1, album1_2_1001, album1_2_1002, album2_1, album2_null);
    }

    @Test
    public void shouldBuildSearchTerms() throws Exception {

        Artist artist1 = new Artist();
        artist1.setName("ar1");
        Artist artistNull = new Artist();
        artistNull.setName(null);

        Album album1 = new Album(artist1);
        album1.setName("al1");
        Album albumNull = new Album(artistNull);
        albumNull.setName(null);

        assertThat(album1.getSearchTerms()).isEqualTo("al1 ar1");
        assertThat(albumNull.getSearchTerms()).isEqualTo(" ");
    }

    @Test
    public void shouldFailOnNotNullViolation() throws Exception {
        
        assertThatThrownBy(() -> new Album(null)).isInstanceOf(NullPointerException.class);
        
        final Album album = new Album(new Artist());
        assertThatThrownBy(() -> album.setArtist(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

        Artist artist = new Artist();

        Album eqAlbum1 = new Album(artist);
        eqAlbum1.setId(1L);
        Album eqAlbum2 = new Album(artist);
        eqAlbum2.setId(1L);
        Album diffAlbum = new Album(artist);
        diffAlbum.setId(2L);

        assertThat(eqAlbum1.hashCode()).isEqualTo(eqAlbum2.hashCode());
        assertThat(eqAlbum1.hashCode()).isNotEqualTo(diffAlbum.hashCode());

        assertThat(eqAlbum1).isEqualTo(eqAlbum1);
        assertThat(eqAlbum1).isEqualTo(eqAlbum2);

        assertThat(eqAlbum1).isNotEqualTo(diffAlbum);
        assertThat(eqAlbum1).isNotEqualTo("foo1");
        assertThat(eqAlbum1).isNotEqualTo(null);
    }
}
