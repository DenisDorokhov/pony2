package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicArtistInfo2 {

    private String biography;
    private String musicBrainzId;
    private String lastFmUrl;
    private String smallImageUrl;
    private String mediumImageUrl;
    private String largeImageUrl;
    private List<OpenSubsonicArtistID3> similarArtist;

    public String getBiography() {
        return biography;
    }

    public OpenSubsonicArtistInfo2 setBiography(String biography) {
        this.biography = biography;
        return this;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public OpenSubsonicArtistInfo2 setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
        return this;
    }

    public String getLastFmUrl() {
        return lastFmUrl;
    }

    public OpenSubsonicArtistInfo2 setLastFmUrl(String lastFmUrl) {
        this.lastFmUrl = lastFmUrl;
        return this;
    }

    public String getSmallImageUrl() {
        return smallImageUrl;
    }

    public OpenSubsonicArtistInfo2 setSmallImageUrl(String smallImageUrl) {
        this.smallImageUrl = smallImageUrl;
        return this;
    }

    public String getMediumImageUrl() {
        return mediumImageUrl;
    }

    public OpenSubsonicArtistInfo2 setMediumImageUrl(String mediumImageUrl) {
        this.mediumImageUrl = mediumImageUrl;
        return this;
    }

    public String getLargeImageUrl() {
        return largeImageUrl;
    }

    public OpenSubsonicArtistInfo2 setLargeImageUrl(String largeImageUrl) {
        this.largeImageUrl = largeImageUrl;
        return this;
    }

    public List<OpenSubsonicArtistID3> getSimilarArtist() {
        return similarArtist;
    }

    public OpenSubsonicArtistInfo2 setSimilarArtist(List<OpenSubsonicArtistID3> similarArtist) {
        this.similarArtist = similarArtist;
        return this;
    }
}
