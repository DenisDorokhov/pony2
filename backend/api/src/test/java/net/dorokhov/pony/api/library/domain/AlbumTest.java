package net.dorokhov.pony.api.library.domain;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class AlbumTest {

    @Test
    public void shouldSort() {

        Artist artist1 = Artist.builder().name("1").build();
        Artist artist2 = Artist.builder().name("2").build();

        Album album1_1 = Album.builder().artist(artist1).name("1_1").year(1000).build();
        Album album1_2_1002 = Album.builder().artist(artist1).name("1_2").year(1002).build();
        Album album1_2_1001 = Album.builder().artist(artist1).name("1_2").year(1001).build();

        Album album2_1 = Album.builder().artist(artist2).name("2_1").build();
        Album album2_null = Album.builder().artist(artist2).name(null).build();

        Album[] list = {album2_null, album2_1, album1_2_1002, album1_2_1001, album1_1};
        Arrays.sort(list);

        assertThat(list).containsExactly(album1_1, album1_2_1001, album1_2_1002, album2_1, album2_null);
    }

    @Test
    public void shouldBuildSearchTerms() {

        Artist artist1 = Artist.builder().name("ar1").build();
        Artist artistNull = Artist.builder().name(null).build();

        Album album1 = Album.builder().artist(artist1).name("al1").build();
        Album albumNull = Album.builder().artist(artistNull).name(null).build();

        assertThat(album1.getSearchTerms()).isEqualTo("al1 ar1");
        assertThat(albumNull.getSearchTerms()).isEqualTo(" ");
    }

    @Test
    public void shouldSupportEqualityAndHashCode() {

        Artist artist = new Artist();

        Album eqAlbum1 = Album.builder().id(1L).artist(artist).build();
        Album eqAlbum2 = Album.builder().id(1L).artist(artist).build();
        Album diffAlbum = Album.builder().id(2L).artist(artist).build();

        assertThat(eqAlbum1.hashCode()).isEqualTo(eqAlbum2.hashCode());
        assertThat(eqAlbum1.hashCode()).isNotEqualTo(diffAlbum.hashCode());

        assertThat(eqAlbum1).isEqualTo(eqAlbum1);
        assertThat(eqAlbum1).isEqualTo(eqAlbum2);

        assertThat(eqAlbum1).isNotEqualTo(diffAlbum);
        assertThat(eqAlbum1).isNotEqualTo("foo1");
        assertThat(eqAlbum1).isNotEqualTo(null);
    }
}
