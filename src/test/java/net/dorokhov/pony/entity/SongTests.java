package net.dorokhov.pony.entity;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SongTests {

    @Test
    public void sort() throws Exception {

        Artist artist1 = new Artist();
        artist1.setName("1");
        Artist artist2 = new Artist();
        artist2.setName("2");

        Album album1_1 = new Album(artist1);
        album1_1.setName("1");
        Album album1_2 = new Album(artist2);
        album1_2.setName("2");
        
        Genre genre = new Genre();

        Song song1_1_1 = new Song(album1_1, genre);
        song1_1_1.setDiscNumber(1);
        song1_1_1.setTrackNumber(1);
        song1_1_1.setName("1");

        Song song1_1_2 = new Song(album1_1, genre);
        song1_1_2.setDiscNumber(1);
        song1_1_2.setTrackNumber(2);
        song1_1_2.setName("2");

        Song song1_2_1 = new Song(album1_2, genre);
        song1_2_1.setDiscNumber(1);
        song1_2_1.setTrackNumber(1);
        song1_2_1.setName("1");

        Song song1_2_2 = new Song(album1_2, genre);
        song1_2_2.setDiscNumber(2);
        song1_2_2.setTrackNumber(1);
        song1_2_2.setName("2");

        Song song1_2_3 = new Song(album1_2, genre);
        song1_2_3.setDiscNumber(2);
        song1_2_3.setTrackNumber(null);
        song1_2_3.setName("2");

        Song[] list = {song1_2_3, song1_2_2, song1_2_1, song1_1_2, song1_1_1};
        Arrays.sort(list);

        assertThat(list).containsExactly(song1_1_1, song1_1_2, song1_2_1, song1_2_2, song1_2_3);
    }

    @Test
    public void shouldBuildSearchTerms() throws Exception {

        Song song1 = new Song();
        song1.setName("s1");
        song1.setArtistName("ar1");
        song1.setAlbumArtistName("ar2");
        song1.setAlbumName("al1");
        assertThat(song1.getSearchTerms()).isEqualTo("s1 ar1 ar2 al1");

        Song song2 = new Song();
        assertThat(song2.getSearchTerms()).isEqualTo("   ");
    }

    @Test
    public void supportEqualityAndHashCode() throws Exception {

        Song eqSong1 = new Song();
        eqSong1.setId(1L);
        Song eqSong2 = new Song();
        eqSong2.setId(1L);
        Song diffSong = new Song();
        diffSong.setId(2L);

        assertThat(eqSong1.hashCode()).isEqualTo(eqSong2.hashCode());
        assertThat(eqSong1.hashCode()).isNotEqualTo(diffSong.hashCode());

        assertThat(eqSong1).isEqualTo(eqSong1);
        assertThat(eqSong1).isEqualTo(eqSong2);

        assertThat(eqSong1).isNotEqualTo(diffSong);
        assertThat(eqSong1).isNotEqualTo("foo1");
        assertThat(eqSong1).isNotEqualTo(null);
    }
}
