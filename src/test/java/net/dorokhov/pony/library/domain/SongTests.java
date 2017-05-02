package net.dorokhov.pony.library.domain;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SongTests {

    @Test
    public void sort() throws Exception {

        Artist artist1 = Artist.builder().name("1").build();
        Artist artist2 = Artist.builder().name("2").build();

        Album album1_1 = Album.builder().artist(artist1).name("1").build();
        Album album1_2 = Album.builder().artist(artist2).name("2").build();
        
        Genre genre = new Genre();

        Song song1_1_1 = buildSong()
                .album(album1_1)
                .genre(genre)
                .discNumber(1)
                .trackNumber(1)
                .name("1")
                .build();

        Song song1_1_2 = buildSong()
                .album(album1_1)
                .genre(genre)
                .discNumber(1)
                .trackNumber(2)
                .name("2")
                .build();

        Song song1_2_1 = buildSong()
                .album(album1_2)
                .genre(genre)
                .discNumber(1)
                .trackNumber(1)
                .name("1")
                .build();

        Song song1_2_2 = buildSong()
                .album(album1_2)
                .genre(genre)
                .discNumber(2)
                .trackNumber(1)
                .name("2")
                .build();

        Song song1_2_3 = buildSong()
                .album(album1_2)
                .genre(genre)
                .discNumber(2)
                .name("2")
                .build();

        Song[] list = {song1_2_3, song1_2_2, song1_2_1, song1_1_2, song1_1_1};
        Arrays.sort(list);

        assertThat(list).containsExactly(song1_1_1, song1_1_2, song1_2_1, song1_2_2, song1_2_3);
    }

    @Test
    public void buildSearchTerms() throws Exception {

        Song song1 = buildSong()
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
    public void supportEqualityAndHashCode() throws Exception {

        Song eqSong1 = buildSong().id(1L).build();
        Song eqSong2 = buildSong().id(1L).build();
        Song diffSong = buildSong().id(2L).build();

        assertThat(eqSong1.hashCode()).isEqualTo(eqSong2.hashCode());
        assertThat(eqSong1.hashCode()).isNotEqualTo(diffSong.hashCode());

        assertThat(eqSong1).isEqualTo(eqSong1);
        assertThat(eqSong1).isEqualTo(eqSong2);

        assertThat(eqSong1).isNotEqualTo(diffSong);
        assertThat(eqSong1).isNotEqualTo("foo1");
        assertThat(eqSong1).isNotEqualTo(null);
    }

    @Test
    public void stringify() throws Exception {
        assertThat(buildSong().build().toString()).startsWith("Song{");
    }
    
    private Song.Builder buildSong() {
        Artist artist = Artist.builder().build();
        Album album = Album.builder().artist(artist).build();
        Genre genre = Genre.builder().build();
        return Song.builder()
                .id(1L)
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
