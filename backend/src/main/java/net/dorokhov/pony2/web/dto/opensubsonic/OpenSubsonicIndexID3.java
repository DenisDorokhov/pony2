package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicIndexID3 {

    private String name;
    private List<OpenSubsonicArtistID3> artist;

    public String getName() {
        return name;
    }

    public OpenSubsonicIndexID3 setName(String name) {
        this.name = name;
        return this;
    }

    public List<OpenSubsonicArtistID3> getArtist() {
        return artist;
    }

    public OpenSubsonicIndexID3 setArtist(List<OpenSubsonicArtistID3> artist) {
        this.artist = artist;
        return this;
    }
}
