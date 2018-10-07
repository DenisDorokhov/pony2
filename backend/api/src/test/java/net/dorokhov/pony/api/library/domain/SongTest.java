package net.dorokhov.pony.api.library.domain;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SongTest {

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

        Album album1_1 = Album.builder()
                .artist(artist1)
                .id("1")
                .name("1")
                .build();
        Album album1_2 = Album.builder()
                .artist(artist2)
                .id("2")
                .name("2")
                .build();
        
        Genre genre = new Genre();

        Song song1_1_1 = songBuilder()
                .id("1_1_1")
                .album(album1_1)
                .genre(genre)
                .discNumber(1)
                .trackNumber(1)
                .name("1")
                .build();
        Song song1_1_2 = songBuilder()
                .id("1_1_2")
                .album(album1_1)
                .genre(genre)
                .discNumber(1)
                .trackNumber(2)
                .name("2")
                .build();

        Song song1_2_1 = songBuilder()
                .id("1_2_1")
                .album(album1_2)
                .genre(genre)
                .discNumber(null)
                .trackNumber(3)
                .name("1")
                .build();
        Song song1_2_2 = songBuilder()
                .id("1_2_2")
                .album(album1_2)
                .genre(genre)
                .discNumber(1)
                .trackNumber(4)
                .name("2")
                .build();
        Song song1_2_3 = songBuilder()
                .id("1_2_3")
                .album(album1_2)
                .genre(genre)
                .discNumber(2)
                .trackNumber(1)
                .name("3")
                .build();
        Song song1_2_4 = songBuilder()
                .id("1_2_4")
                .album(album1_2)
                .genre(genre)
                .discNumber(2)
                .name("4")
                .build();

        Song[] list = {song1_2_4, song1_2_3, song1_2_2, song1_2_1, song1_1_2, song1_1_1};
        Arrays.sort(list);

        assertThat(list).containsExactly(song1_1_1, song1_1_2, song1_2_1, song1_2_2, song1_2_3, song1_2_4);
    }

    @Test
    public void shouldBuildSearchTerms() {

        Song song1 = songBuilder()
                .name("s1")
                .artistName("ar1")
                .albumArtistName("ar2")
                .albumName("al1")
                .build();

        assertThat(song1.getSearchTerms()).isEqualTo("s1 ar1 ar2 al1");

        Song song2 = new Song();
        assertThat(song2.getSearchTerms()).isEqualTo("   ");
    }

    @Test
    public void shouldSupportEqualityAndHashCode() {

        Song eqSong1 = songBuilder().id("1").build();
        Song eqSong2 = songBuilder().id("1").build();
        Song diffSong = songBuilder().id("2").build();

        assertThat(eqSong1.hashCode()).isEqualTo(eqSong2.hashCode());
        assertThat(eqSong1.hashCode()).isNotEqualTo(diffSong.hashCode());

        assertThat(eqSong1).isEqualTo(eqSong1);
        assertThat(eqSong1).isEqualTo(eqSong2);

        assertThat(eqSong1).isNotEqualTo(diffSong);
        assertThat(eqSong1).isNotEqualTo("foo1");
        assertThat(eqSong1).isNotEqualTo(null);
    }
    
    private Song.Builder songBuilder() {
        Artist artist = Artist.builder().build();
        Album album = Album.builder().artist(artist).build();
        Genre genre = Genre.builder().build();
        return Song.builder()
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .path("somePath")
                .fileType(FileType.of("text/plain", "txt"))
                .size(123L)
                .duration(234L)
                .bitRate(345L)
                .bitRateVariable(true)
                .discNumber(1)
                .discCount(2)
                .trackNumber(3)
                .trackCount(10)
                .name("someSong")
                .genreName("someGenre")
                .artistName("someArtist")
                .albumArtistName("someAlbumArtist")
                .albumName("someAlbum")
                .year(1986)
                .artwork(null)
                .album(album)
                .genre(genre);
    }
}
