package net.dorokhov.pony.entity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SongTests {

    @Test
    public void songShouldBeSorted() throws Exception {

        Artist artist1 = new Artist();
        artist1.setName("1");
        Artist artist2 = new Artist();
        artist2.setName("2");

        Album album1_1 = new Album(artist1);
        album1_1.setName("1");
        Album album1_2 = new Album(artist2);
        album1_2.setName("2");

        Song song1_1_1 = getSongBuilder()
                .setAlbum(album1_1)
                .setDiscNumber(1)
                .setTrackNumber(1)
                .setName("1")
                .build();
        Song song1_1_2 = getSongBuilder()
                .setAlbum(album1_1)
                .setDiscNumber(1)
                .setTrackNumber(2)
                .setName("2")
                .build();
        Song song1_2_1 = getSongBuilder()
                .setAlbum(album1_2)
                .setDiscNumber(1)
                .setTrackNumber(1)
                .setName("1")
                .build();
        Song song1_2_2 = getSongBuilder()
                .setAlbum(album1_2)
                .setDiscNumber(2)
                .setTrackNumber(1)
                .setName("2")
                .build();
        Song song1_2_2_genreNull = getSongBuilder()
                .setAlbum(album1_2)
                .setDiscNumber(2)
                .setTrackNumber(null)
                .setName("2")
                .build();

        Song[] list = {song1_2_2_genreNull, song1_2_2, song1_2_1, song1_1_2, song1_1_1};
        Arrays.sort(list);

        assertThat(list).containsExactly(song1_1_1, song1_1_2, song1_2_1, song1_2_2, song1_2_2_genreNull);
    }

    @Test
    public void songShouldBuildSearchTerms() throws Exception {

        Artist artist = new Artist();
        Album album = new Album(artist);

        Song song1 = new Song.Builder()
                .setPath("path")
                .setFormat("format")
                .setMimeType("mime")
                .setSize(10L)
                .setDuration(100)
                .setBitRate(1000L)
                .setAlbum(album)
                .setGenre(new Genre())
                .setName("s1")
                .setArtistName("ar1")
                .setAlbumArtistName("ar2")
                .setAlbumName("al1")
                .build();
        assertThat(song1.getSearchTerms()).isEqualTo("s1 ar1 ar2 al1");

        Song song2 = new Song.Builder()
                .setPath("path")
                .setFormat("format")
                .setMimeType("mime")
                .setSize(10L)
                .setDuration(100)
                .setBitRate(1000L)
                .setAlbum(album)
                .setGenre(new Genre())
                .build();
        assertThat(song2.getSearchTerms()).isEqualTo("   ");
    }

    @Test
    public void songShouldFailOnNotNullViolation() throws Exception {

        Song song = getSongBuilder().build();

        assertThatThrownBy(() -> new Song.Builder(song).setPath(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Song.Builder(song).setFormat(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Song.Builder(song).setMimeType(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Song.Builder(song).setSize(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Song.Builder(song).setDuration(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Song.Builder(song).setBitRate(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Song.Builder(song).setAlbum(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Song.Builder(song).setGenre(null).build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void songShouldBeBuilt() throws Exception {

        Artist artist = new Artist();
        artist.setName("ar1");
        Album album = new Album(artist);
        album.setName("al1");
        Genre genre = new Genre();
        genre.setName("g1");
        StoredFile artwork = new StoredFile.Builder()
                .setName("")
                .setMimeType("")
                .setChecksum("")
                .setSize(0L)
                .setPath("")
                .build();

        Song song = new Song.Builder()
                .setId(1L)
                .setPath("path")
                .setFormat("format")
                .setMimeType("mime")
                .setSize(10L)
                .setDuration(100)
                .setBitRate(1000L)
                .setAlbum(album)
                .setGenre(genre)
                .setDiscNumber(20)
                .setDiscCount(2)
                .setTrackNumber(3)
                .setTrackCount(40)
                .setName("name")
                .setGenreName("g1")
                .setArtistName("ar1")
                .setAlbumArtistName("aar1")
                .setAlbumName("al1")
                .setYear(1986)
                .setArtwork(artwork)
                .build();

        assertThat(song.getId()).isEqualTo(Optional.of(1L));
        assertThat(song.getCreationDate().orElse(null)).isNull();
        assertThat(song.getUpdateDate().orElse(null)).isNull();
        assertThat(song.getPath()).isEqualTo("path");
        assertThat(song.getFormat()).isEqualTo("format");
        assertThat(song.getMimeType()).isEqualTo("mime");
        assertThat(song.getSize()).isEqualTo(10L);
        assertThat(song.getDuration()).isEqualTo(100);
        assertThat(song.getBitRate()).isEqualTo(1000L);
        assertThat(song.getAlbum()).isEqualTo(album);
        assertThat(song.getGenre()).isEqualTo(genre);
        assertThat(song.getDiscNumber()).isEqualTo(Optional.of(20));
        assertThat(song.getDiscCount()).isEqualTo(Optional.of(2));
        assertThat(song.getTrackNumber()).isEqualTo(Optional.of(3));
        assertThat(song.getTrackCount()).isEqualTo(Optional.of(40));
        assertThat(song.getName()).isEqualTo(Optional.of("name"));
        assertThat(song.getGenreName()).isEqualTo(Optional.of("g1"));
        assertThat(song.getArtistName()).isEqualTo(Optional.of("ar1"));
        assertThat(song.getAlbumArtistName()).isEqualTo(Optional.of("aar1"));
        assertThat(song.getAlbumName()).isEqualTo(Optional.of("al1"));
        assertThat(song.getYear()).isEqualTo(Optional.of(1986));
        assertThat(song.getArtwork()).isEqualTo(Optional.of(artwork));
    }

    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

        Song eqSong1 = getSongBuilder().setId(1L).build();
        Song eqSong2 = getSongBuilder().setId(1L).build();
        Song diffSong = getSongBuilder().setId(2L).build();

        assertThat(eqSong1.hashCode()).isEqualTo(eqSong2.hashCode());
        assertThat(eqSong1.hashCode()).isNotEqualTo(diffSong.hashCode());

        assertThat(eqSong1).isEqualTo(eqSong1);
        assertThat(eqSong1).isEqualTo(eqSong2);

        assertThat(eqSong1).isNotEqualTo(diffSong);
        assertThat(eqSong1).isNotEqualTo("foo1");
        assertThat(eqSong1).isNotEqualTo(null);
    }

    private Song.Builder getSongBuilder() {
        return new Song.Builder()
                .setPath("")
                .setFormat("")
                .setMimeType("")
                .setSize(1L)
                .setDuration(1)
                .setBitRate(1L)
                .setAlbum(new Album(new Artist()))
                .setGenre(new Genre());
    }
}
