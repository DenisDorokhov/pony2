package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicStarred2 {

    private List<?> artist;
    private List<?> album;
    private List<OpenSubsonicChild> song;

    public List<?> getArtist() {
        return artist;
    }

    public OpenSubsonicStarred2 setArtist(List<?> artist) {
        this.artist = artist;
        return this;
    }

    public List<?> getAlbum() {
        return album;
    }

    public OpenSubsonicStarred2 setAlbum(List<?> album) {
        this.album = album;
        return this;
    }

    public List<OpenSubsonicChild> getSong() {
        return song;
    }

    public OpenSubsonicStarred2 setSong(List<OpenSubsonicChild> song) {
        this.song = song;
        return this;
    }
}
