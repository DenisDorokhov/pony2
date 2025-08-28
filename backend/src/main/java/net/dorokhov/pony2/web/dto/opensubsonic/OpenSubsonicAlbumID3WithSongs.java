package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicAlbumID3WithSongs {

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
    private List<OpenSubsonicItemGenre> genres;
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
    private List<OpenSubsonicChild> song;

    public String getId() {
        return id;
    }

    public OpenSubsonicAlbumID3WithSongs setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public OpenSubsonicAlbumID3WithSongs setName(String name) {
        this.name = name;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public OpenSubsonicAlbumID3WithSongs setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getArtist() {
        return artist;
    }

    public OpenSubsonicAlbumID3WithSongs setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getArtistId() {
        return artistId;
    }

    public OpenSubsonicAlbumID3WithSongs setArtistId(String artistId) {
        this.artistId = artistId;
        return this;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public OpenSubsonicAlbumID3WithSongs setCoverArt(String coverArt) {
        this.coverArt = coverArt;
        return this;
    }

    public int getSongCount() {
        return songCount;
    }

    public OpenSubsonicAlbumID3WithSongs setSongCount(int songCount) {
        this.songCount = songCount;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public OpenSubsonicAlbumID3WithSongs setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public OpenSubsonicAlbumID3WithSongs setPlayCount(Long playCount) {
        this.playCount = playCount;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public OpenSubsonicAlbumID3WithSongs setCreated(String created) {
        this.created = created;
        return this;
    }

    public String getStarred() {
        return starred;
    }

    public OpenSubsonicAlbumID3WithSongs setStarred(String starred) {
        this.starred = starred;
        return this;
    }

    public Integer getYear() {
        return year;
    }

    public OpenSubsonicAlbumID3WithSongs setYear(Integer year) {
        this.year = year;
        return this;
    }

    public String getGenre() {
        return genre;
    }

    public OpenSubsonicAlbumID3WithSongs setGenre(String genre) {
        this.genre = genre;
        return this;
    }

    public String getPlayed() {
        return played;
    }

    public OpenSubsonicAlbumID3WithSongs setPlayed(String played) {
        this.played = played;
        return this;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public OpenSubsonicAlbumID3WithSongs setUserRating(Integer userRating) {
        this.userRating = userRating;
        return this;
    }

    public List<OpenSubsonicRecordLabel> getRecordLabels() {
        return recordLabels;
    }

    public OpenSubsonicAlbumID3WithSongs setRecordLabels(List<OpenSubsonicRecordLabel> recordLabels) {
        this.recordLabels = recordLabels;
        return this;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public OpenSubsonicAlbumID3WithSongs setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
        return this;
    }

    public List<OpenSubsonicItemGenre> getGenres() {
        return genres;
    }

    public OpenSubsonicAlbumID3WithSongs setGenres(List<OpenSubsonicItemGenre> genres) {
        this.genres = genres;
        return this;
    }

    public List<OpenSubsonicArtistID3> getArtists() {
        return artists;
    }

    public OpenSubsonicAlbumID3WithSongs setArtists(List<OpenSubsonicArtistID3> artists) {
        this.artists = artists;
        return this;
    }

    public String getDisplayArtist() {
        return displayArtist;
    }

    public OpenSubsonicAlbumID3WithSongs setDisplayArtist(String displayArtist) {
        this.displayArtist = displayArtist;
        return this;
    }

    public List<String> getReleaseTypes() {
        return releaseTypes;
    }

    public OpenSubsonicAlbumID3WithSongs setReleaseTypes(List<String> releaseTypes) {
        this.releaseTypes = releaseTypes;
        return this;
    }

    public List<String> getMoods() {
        return moods;
    }

    public OpenSubsonicAlbumID3WithSongs setMoods(List<String> moods) {
        this.moods = moods;
        return this;
    }

    public String getSortName() {
        return sortName;
    }

    public OpenSubsonicAlbumID3WithSongs setSortName(String sortName) {
        this.sortName = sortName;
        return this;
    }

    public OpenSubsonicItemDate getOriginalReleaseDate() {
        return originalReleaseDate;
    }

    public OpenSubsonicAlbumID3WithSongs setOriginalReleaseDate(OpenSubsonicItemDate originalReleaseDate) {
        this.originalReleaseDate = originalReleaseDate;
        return this;
    }

    public OpenSubsonicItemDate getReleaseDate() {
        return releaseDate;
    }

    public OpenSubsonicAlbumID3WithSongs setReleaseDate(OpenSubsonicItemDate releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }

    public Boolean getCompilation() {
        return isCompilation;
    }

    public OpenSubsonicAlbumID3WithSongs setCompilation(Boolean compilation) {
        isCompilation = compilation;
        return this;
    }

    public String getExplicitStatus() {
        return explicitStatus;
    }

    public OpenSubsonicAlbumID3WithSongs setExplicitStatus(String explicitStatus) {
        this.explicitStatus = explicitStatus;
        return this;
    }

    public List<OpenSubsonicDiscTitle> getDiscTitles() {
        return discTitles;
    }

    public OpenSubsonicAlbumID3WithSongs setDiscTitles(List<OpenSubsonicDiscTitle> discTitles) {
        this.discTitles = discTitles;
        return this;
    }

    public List<OpenSubsonicChild> getSong() {
        return song;
    }

    public OpenSubsonicAlbumID3WithSongs setSong(List<OpenSubsonicChild> song) {
        this.song = song;
        return this;
    }
}
