package net.dorokhov.pony3.api.library.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class AlbumTest {

    @Test
    public void shouldSort() {

        Artist artist1 = new Artist()
                .setId("1")
                .setName("1");
        Artist artist2 = new Artist()
                .setId("2")
                .setName("2");

        Album album1_1 = new Album()
                .setArtist(artist1)
                .setId("1_1")
                .setName("1_1")
                .setYear(1000);
        Album album1_2_1002 = new Album()
                .setArtist(artist1)
                .setId("1_2_1002")
                .setName("1_2")
                .setYear(1002);
        Album album1_2_1001 = new Album()
                .setArtist(artist1)
                .setName("1_2_1001")
                .setYear(1001);

        Album album2_1 = new Album()
                .setArtist(artist2)
                .setId("2_1")
                .setName("2_1");
        Album album2_null = new Album()
                .setArtist(artist2)
                .setId("2_null")
                .setName(null);

        Album[] list = {album2_null, album2_1, album1_2_1002, album1_2_1001, album1_1};
        Arrays.sort(list);

        assertThat(list).containsExactly(album1_1, album1_2_1001, album1_2_1002, album2_1, album2_null);
    }

    @Test
    public void shouldBuildSearchTerms() {

        Artist artist1 = new Artist().setName("ar1");
        Artist artistNull = new Artist().setName(null);

        Album album1 = new Album().setArtist(artist1).setName("al1");
        Album albumNull = new Album().setArtist(artistNull).setName(null);

        assertThat(album1.getSearchTerms()).isEqualTo("al1 ar1");
        assertThat(albumNull.getSearchTerms()).isEqualTo(" ");
    }

    @Test
    public void shouldSupportEqualityAndHashCode() {

        Artist artist = new Artist();

        Album eqAlbum1 = new Album().setId("1").setArtist(artist);
        Album eqAlbum2 = new Album().setId("1").setArtist(artist);
        Album diffAlbum = new Album().setId("2").setArtist(artist);

        assertThat(eqAlbum1.hashCode()).isEqualTo(eqAlbum2.hashCode());
        assertThat(eqAlbum1.hashCode()).isNotEqualTo(diffAlbum.hashCode());

        assertThat(eqAlbum1).isEqualTo(eqAlbum1);
        assertThat(eqAlbum1).isEqualTo(eqAlbum2);

        assertThat(eqAlbum1).isNotEqualTo(diffAlbum);
        assertThat(eqAlbum1).isNotEqualTo("foo1");
        assertThat(eqAlbum1).isNotEqualTo(null);
    }
}
