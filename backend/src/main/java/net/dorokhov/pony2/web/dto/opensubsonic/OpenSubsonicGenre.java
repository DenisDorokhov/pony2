package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicGenre {

    private int songCount;
    private int albumCount;
    private String value;

    public int getSongCount() {
        return songCount;
    }

    public OpenSubsonicGenre setSongCount(int songCount) {
        this.songCount = songCount;
        return this;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public OpenSubsonicGenre setAlbumCount(int albumCount) {
        this.albumCount = albumCount;
        return this;
    }

    public String getValue() {
        return value;
    }

    public OpenSubsonicGenre setValue(String value) {
        this.value = value;
        return this;
    }
}
