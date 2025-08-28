package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.ArrayList;
import java.util.List;

public class OpenSubsonicArtistID3 {

    private String id;
    private String name;
    private String coverArt;
    private String artistImageUrl;
    private int albumCount;
    private String starred;
    private String musicBrainzId;
    private String sortName;
    private List<String> roles = new ArrayList<>();

    public String getId() {
        return id;
    }

    public OpenSubsonicArtistID3 setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public OpenSubsonicArtistID3 setName(String name) {
        this.name = name;
        return this;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public OpenSubsonicArtistID3 setCoverArt(String coverArt) {
        this.coverArt = coverArt;
        return this;
    }

    public String getArtistImageUrl() {
        return artistImageUrl;
    }

    public OpenSubsonicArtistID3 setArtistImageUrl(String artistImageUrl) {
        this.artistImageUrl = artistImageUrl;
        return this;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public OpenSubsonicArtistID3 setAlbumCount(int albumCount) {
        this.albumCount = albumCount;
        return this;
    }

    public String getStarred() {
        return starred;
    }

    public OpenSubsonicArtistID3 setStarred(String starred) {
        this.starred = starred;
        return this;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public OpenSubsonicArtistID3 setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
        return this;
    }

    public String getSortName() {
        return sortName;
    }

    public OpenSubsonicArtistID3 setSortName(String sortName) {
        this.sortName = sortName;
        return this;
    }

    public List<String> getRoles() {
        return roles;
    }

    public OpenSubsonicArtistID3 setRoles(List<String> roles) {
        this.roles = roles;
        return this;
    }
}
