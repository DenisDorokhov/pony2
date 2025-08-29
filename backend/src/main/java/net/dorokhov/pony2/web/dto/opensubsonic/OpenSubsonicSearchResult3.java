package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicSearchResult3 {

    private List<OpenSubsonicArtistID3> artist;
    private List<OpenSubsonicAlbumID3> album;
    private List<OpenSubsonicChild> song;

    public List<OpenSubsonicArtistID3> getArtist() {
        return artist;
    }

    public OpenSubsonicSearchResult3 setArtist(List<OpenSubsonicArtistID3> artist) {
        this.artist = artist;
        return this;
    }

    public List<OpenSubsonicAlbumID3> getAlbum() {
        return album;
    }

    public OpenSubsonicSearchResult3 setAlbum(List<OpenSubsonicAlbumID3> album) {
        this.album = album;
        return this;
    }

    public List<OpenSubsonicChild> getSong() {
        return song;
    }

    public OpenSubsonicSearchResult3 setSong(List<OpenSubsonicChild> song) {
        this.song = song;
        return this;
    }
}
