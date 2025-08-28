package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicContributor {

    private String role;
    private String subRole;
    private OpenSubsonicArtistID3 artist;

    public String getRole() {
        return role;
    }

    public OpenSubsonicContributor setRole(String role) {
        this.role = role;
        return this;
    }

    public String getSubRole() {
        return subRole;
    }

    public OpenSubsonicContributor setSubRole(String subRole) {
        this.subRole = subRole;
        return this;
    }

    public OpenSubsonicArtistID3 getArtist() {
        return artist;
    }

    public OpenSubsonicContributor setArtist(OpenSubsonicArtistID3 artist) {
        this.artist = artist;
        return this;
    }
}
