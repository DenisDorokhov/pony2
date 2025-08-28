package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicAlbumInfo {

    private String notes;
    private String musicBrainzId;
    private String lastFmUrl;
    private String smallImageUrl;
    private String mediumImageUrl;
    private String largeImageUrl;

    public String getNotes() {
        return notes;
    }

    public OpenSubsonicAlbumInfo setNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public OpenSubsonicAlbumInfo setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
        return this;
    }

    public String getLastFmUrl() {
        return lastFmUrl;
    }

    public OpenSubsonicAlbumInfo setLastFmUrl(String lastFmUrl) {
        this.lastFmUrl = lastFmUrl;
        return this;
    }

    public String getSmallImageUrl() {
        return smallImageUrl;
    }

    public OpenSubsonicAlbumInfo setSmallImageUrl(String smallImageUrl) {
        this.smallImageUrl = smallImageUrl;
        return this;
    }

    public String getMediumImageUrl() {
        return mediumImageUrl;
    }

    public OpenSubsonicAlbumInfo setMediumImageUrl(String mediumImageUrl) {
        this.mediumImageUrl = mediumImageUrl;
        return this;
    }

    public String getLargeImageUrl() {
        return largeImageUrl;
    }

    public OpenSubsonicAlbumInfo setLargeImageUrl(String largeImageUrl) {
        this.largeImageUrl = largeImageUrl;
        return this;
    }
}
