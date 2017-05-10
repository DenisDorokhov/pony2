package net.dorokhov.pony.fixture;

import net.dorokhov.pony.library.domain.*;

import java.time.LocalDateTime;

public final class SongFixtures {

    private SongFixtures() {
    }
    
    public static Song get() {
        return builder().build();
    }
    
    public static Song.Builder builder() {
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
