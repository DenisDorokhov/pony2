package net.dorokhov.pony2.test;

import net.dorokhov.pony2.api.library.domain.*;

import java.time.LocalDateTime;

public final class SongFixtures {

    private SongFixtures() {
    }

    public static Song song() {
        Artist artist = new Artist();
        Album album = new Album().setArtist(artist);
        Genre genre = new Genre();
        return new Song()
                .setId("1")
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
