package net.dorokhov.pony3.api.library.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SongTest {

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
                .setId("1")
                .setName("1");
        Album album1_2 = new Album()
                .setArtist(artist2)
                .setId("2")
                .setName("2");
        
        Genre genre = new Genre();

        Song song1_1_1 = song()
                .setId("1_1_1")
                .setAlbum(album1_1)
                .setGenre(genre)
                .setDiscNumber(1)
                .setTrackNumber(1)
                .setName("1");
        Song song1_1_2 = song()
                .setId("1_1_2")
                .setAlbum(album1_1)
                .setGenre(genre)
                .setDiscNumber(1)
                .setTrackNumber(2)
                .setName("2");

        Song song1_2_1 = song()
                .setId("1_2_1")
                .setAlbum(album1_2)
                .setGenre(genre)
                .setDiscNumber(null)
                .setTrackNumber(3)
                .setName("1");
        Song song1_2_2 = song()
                .setId("1_2_2")
                .setAlbum(album1_2)
                .setGenre(genre)
                .setDiscNumber(1)
                .setTrackNumber(4)
                .setName("2");
        Song song1_2_3 = song()
                .setId("1_2_3")
                .setAlbum(album1_2)
                .setGenre(genre)
                .setDiscNumber(2)
                .setTrackNumber(1)
                .setName("3");
        Song song1_2_4 = song()
                .setId("1_2_4")
                .setAlbum(album1_2)
                .setGenre(genre)
                .setDiscNumber(2)
                .setName("4");

        Song[] list = {song1_2_4, song1_2_3, song1_2_2, song1_2_1, song1_1_2, song1_1_1};
        Arrays.sort(list);

        assertThat(list).containsExactly(song1_1_1, song1_1_2, song1_2_1, song1_2_2, song1_2_3, song1_2_4);
    }

    @Test
    public void shouldBuildSearchTerms() {

        Song song1 = song()
                .setName("s1")
                .setArtistName("ar1")
                .setAlbumArtistName("ar2")
                .setAlbumName("al1");

        assertThat(song1.getSearchTerms()).isEqualTo("s1 ar1 ar2 al1");

        Song song2 = new Song();
        assertThat(song2.getSearchTerms()).isEqualTo("   ");
    }

    @Test
    public void shouldSupportEqualityAndHashCode() {

        Song eqSong1 = song().setId("1");
        Song eqSong2 = song().setId("1");
        Song diffSong = song().setId("2");

        assertThat(eqSong1.hashCode()).isEqualTo(eqSong2.hashCode());
        assertThat(eqSong1.hashCode()).isNotEqualTo(diffSong.hashCode());

        assertThat(eqSong1).isEqualTo(eqSong1);
        assertThat(eqSong1).isEqualTo(eqSong2);

        assertThat(eqSong1).isNotEqualTo(diffSong);
        assertThat(eqSong1).isNotEqualTo("foo1");
        assertThat(eqSong1).isNotEqualTo(null);
    }
    
    private Song song() {
        Artist artist = new Artist();
        Album album = new Album().setArtist(artist);
        Genre genre = new Genre();
        return new Song()
                .setCreationDate(LocalDateTime.now())
                .setUpdateDate(LocalDateTime.now())
                .setPath("somePath")
                .setFileType(FileType.of("text/plain", "txt"))
                .setSize(123L)
                .setDuration(234L)
                .setBitRate(345L)
                .setBitRateVariable(true)
                .setDiscNumber(1)
                .setDiscCount(2)
                .setTrackNumber(3)
                .setTrackCount(10)
                .setName("someSong")
                .setGenreName("someGenre")
                .setArtistName("someArtist")
                .setAlbumArtistName("someAlbumArtist")
                .setAlbumName("someAlbum")
                .setYear(1986)
                .setArtwork(null)
                .setAlbum(album)
                .setGenre(genre);
    }
}
