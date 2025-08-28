package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicAlbumID3 {

    private String id;
    private String name;
    private String version;
    private String artist;
    private String artistId;
    private String coverArt;
    private int songCount;
    private int duration;
    private Long playCount;
    private String created;
    private String starred;
    private Integer year;
    private String genre;
    private String played;
    private Integer userRating;
    private List<OpenSubsonicRecordLabel> recordLabels;
    private String musicBrainzId;
    private OpenSubsonicItemGenre genres;
    private List<OpenSubsonicArtistID3> artists;
    private String displayArtist;
    private List<String> releaseTypes;
    private List<String> moods;
    private String sortName;
    private OpenSubsonicItemDate originalReleaseDate;
    private OpenSubsonicItemDate releaseDate;
    private Boolean isCompilation;
    private String explicitStatus;
    private List<OpenSubsonicDiscTitle> discTitles;

    public String getId() {
        return id;
    }

    public OpenSubsonicAlbumID3 setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public OpenSubsonicAlbumID3 setName(String name) {
        this.name = name;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public OpenSubsonicAlbumID3 setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getArtist() {
        return artist;
    }

    public OpenSubsonicAlbumID3 setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getArtistId() {
        return artistId;
    }

    public OpenSubsonicAlbumID3 setArtistId(String artistId) {
        this.artistId = artistId;
        return this;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public OpenSubsonicAlbumID3 setCoverArt(String coverArt) {
        this.coverArt = coverArt;
        return this;
    }

    public int getSongCount() {
        return songCount;
    }

    public OpenSubsonicAlbumID3 setSongCount(int songCount) {
        this.songCount = songCount;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public OpenSubsonicAlbumID3 setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public OpenSubsonicAlbumID3 setPlayCount(Long playCount) {
        this.playCount = playCount;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public OpenSubsonicAlbumID3 setCreated(String created) {
        this.created = created;
        return this;
    }

    public String getStarred() {
        return starred;
    }

    public OpenSubsonicAlbumID3 setStarred(String starred) {
        this.starred = starred;
        return this;
    }

    public Integer getYear() {
        return year;
    }

    public OpenSubsonicAlbumID3 setYear(Integer year) {
        this.year = year;
        return this;
    }

    public String getGenre() {
        return genre;
    }

    public OpenSubsonicAlbumID3 setGenre(String genre) {
        this.genre = genre;
        return this;
    }

    public String getPlayed() {
        return played;
    }

    public OpenSubsonicAlbumID3 setPlayed(String played) {
        this.played = played;
        return this;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public OpenSubsonicAlbumID3 setUserRating(Integer userRating) {
        this.userRating = userRating;
        return this;
    }

    public List<OpenSubsonicRecordLabel> getRecordLabels() {
        return recordLabels;
    }

    public OpenSubsonicAlbumID3 setRecordLabels(List<OpenSubsonicRecordLabel> recordLabels) {
        this.recordLabels = recordLabels;
        return this;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public OpenSubsonicAlbumID3 setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
        return this;
    }

    public OpenSubsonicItemGenre getGenres() {
        return genres;
    }

    public OpenSubsonicAlbumID3 setGenres(OpenSubsonicItemGenre genres) {
        this.genres = genres;
        return this;
    }

    public List<OpenSubsonicArtistID3> getArtists() {
        return artists;
    }

    public OpenSubsonicAlbumID3 setArtists(List<OpenSubsonicArtistID3> artists) {
        this.artists = artists;
        return this;
    }

    public String getDisplayArtist() {
        return displayArtist;
    }

    public OpenSubsonicAlbumID3 setDisplayArtist(String displayArtist) {
        this.displayArtist = displayArtist;
        return this;
    }

    public List<String> getReleaseTypes() {
        return releaseTypes;
    }

    public OpenSubsonicAlbumID3 setReleaseTypes(List<String> releaseTypes) {
        this.releaseTypes = releaseTypes;
        return this;
    }

    public List<String> getMoods() {
        return moods;
    }

    public OpenSubsonicAlbumID3 setMoods(List<String> moods) {
        this.moods = moods;
        return this;
    }

    public String getSortName() {
        return sortName;
    }

    public OpenSubsonicAlbumID3 setSortName(String sortName) {
        this.sortName = sortName;
        return this;
    }

    public OpenSubsonicItemDate getOriginalReleaseDate() {
        return originalReleaseDate;
    }

    public OpenSubsonicAlbumID3 setOriginalReleaseDate(OpenSubsonicItemDate originalReleaseDate) {
        this.originalReleaseDate = originalReleaseDate;
        return this;
    }

    public OpenSubsonicItemDate getReleaseDate() {
        return releaseDate;
    }

    public OpenSubsonicAlbumID3 setReleaseDate(OpenSubsonicItemDate releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }

    public Boolean getCompilation() {
        return isCompilation;
    }

    public OpenSubsonicAlbumID3 setCompilation(Boolean compilation) {
        isCompilation = compilation;
        return this;
    }

    public String getExplicitStatus() {
        return explicitStatus;
    }

    public OpenSubsonicAlbumID3 setExplicitStatus(String explicitStatus) {
        this.explicitStatus = explicitStatus;
        return this;
    }

    public List<OpenSubsonicDiscTitle> getDiscTitles() {
        return discTitles;
    }

    public OpenSubsonicAlbumID3 setDiscTitles(List<OpenSubsonicDiscTitle> discTitles) {
        this.discTitles = discTitles;
        return this;
    }
}
