package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.ArrayList;
import java.util.List;

public class OpenSubsonicArtistWithAlbumsID3 {

    private String id;
    private String name;
    private String coverArt;
    private int albumCount;
    private int userRating;
    private String artistImageUrl;
    private String starred;
    private String musicBrainzId;
    private String sortName;
    private List<String> roles = new ArrayList<>();
    private List<Album> album = new ArrayList<>();

    public String getId() {
        return id;
    }

    public OpenSubsonicArtistWithAlbumsID3 setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public OpenSubsonicArtistWithAlbumsID3 setName(String name) {
        this.name = name;
        return this;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public OpenSubsonicArtistWithAlbumsID3 setCoverArt(String coverArt) {
        this.coverArt = coverArt;
        return this;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public OpenSubsonicArtistWithAlbumsID3 setAlbumCount(int albumCount) {
        this.albumCount = albumCount;
        return this;
    }

    public int getUserRating() {
        return userRating;
    }

    public OpenSubsonicArtistWithAlbumsID3 setUserRating(int userRating) {
        this.userRating = userRating;
        return this;
    }

    public String getArtistImageUrl() {
        return artistImageUrl;
    }

    public OpenSubsonicArtistWithAlbumsID3 setArtistImageUrl(String artistImageUrl) {
        this.artistImageUrl = artistImageUrl;
        return this;
    }

    public String getStarred() {
        return starred;
    }

    public OpenSubsonicArtistWithAlbumsID3 setStarred(String starred) {
        this.starred = starred;
        return this;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public OpenSubsonicArtistWithAlbumsID3 setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
        return this;
    }

    public String getSortName() {
        return sortName;
    }

    public OpenSubsonicArtistWithAlbumsID3 setSortName(String sortName) {
        this.sortName = sortName;
        return this;
    }

    public List<String> getRoles() {
        return roles;
    }

    public OpenSubsonicArtistWithAlbumsID3 setRoles(List<String> roles) {
        this.roles = roles;
        return this;
    }

    public List<Album> getAlbum() {
        return album;
    }

    public OpenSubsonicArtistWithAlbumsID3 setAlbum(List<Album> album) {
        this.album = album;
        return this;
    }

    public static class Album {

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
        private boolean isCompilation;
        private String explicitStatus;
        private List<OpenSubsonicDiscTitle> discTitles;

        public String getId() {
            return id;
        }

        public Album setId(String id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Album setName(String name) {
            this.name = name;
            return this;
        }

        public String getVersion() {
            return version;
        }

        public Album setVersion(String version) {
            this.version = version;
            return this;
        }

        public String getArtist() {
            return artist;
        }

        public Album setArtist(String artist) {
            this.artist = artist;
            return this;
        }

        public String getArtistId() {
            return artistId;
        }

        public Album setArtistId(String artistId) {
            this.artistId = artistId;
            return this;
        }

        public String getCoverArt() {
            return coverArt;
        }

        public Album setCoverArt(String coverArt) {
            this.coverArt = coverArt;
            return this;
        }

        public int getSongCount() {
            return songCount;
        }

        public Album setSongCount(int songCount) {
            this.songCount = songCount;
            return this;
        }

        public int getDuration() {
            return duration;
        }

        public Album setDuration(int duration) {
            this.duration = duration;
            return this;
        }

        public Long getPlayCount() {
            return playCount;
        }

        public Album setPlayCount(Long playCount) {
            this.playCount = playCount;
            return this;
        }

        public String getCreated() {
            return created;
        }

        public Album setCreated(String created) {
            this.created = created;
            return this;
        }

        public String getStarred() {
            return starred;
        }

        public Album setStarred(String starred) {
            this.starred = starred;
            return this;
        }

        public Integer getYear() {
            return year;
        }

        public Album setYear(Integer year) {
            this.year = year;
            return this;
        }

        public String getGenre() {
            return genre;
        }

        public Album setGenre(String genre) {
            this.genre = genre;
            return this;
        }

        public String getPlayed() {
            return played;
        }

        public Album setPlayed(String played) {
            this.played = played;
            return this;
        }

        public Integer getUserRating() {
            return userRating;
        }

        public Album setUserRating(Integer userRating) {
            this.userRating = userRating;
            return this;
        }

        public List<OpenSubsonicRecordLabel> getRecordLabels() {
            return recordLabels;
        }

        public Album setRecordLabels(List<OpenSubsonicRecordLabel> recordLabels) {
            this.recordLabels = recordLabels;
            return this;
        }

        public String getMusicBrainzId() {
            return musicBrainzId;
        }

        public Album setMusicBrainzId(String musicBrainzId) {
            this.musicBrainzId = musicBrainzId;
            return this;
        }

        public List<OpenSubsonicItemGenre> getGenres() {
            return genres;
        }

        public Album setGenres(List<OpenSubsonicItemGenre> genres) {
            this.genres = genres;
            return this;
        }

        public List<OpenSubsonicArtistID3> getArtists() {
            return artists;
        }

        public Album setArtists(List<OpenSubsonicArtistID3> artists) {
            this.artists = artists;
            return this;
        }

        public String getDisplayArtist() {
            return displayArtist;
        }

        public Album setDisplayArtist(String displayArtist) {
            this.displayArtist = displayArtist;
            return this;
        }

        public List<String> getReleaseTypes() {
            return releaseTypes;
        }

        public Album setReleaseTypes(List<String> releaseTypes) {
            this.releaseTypes = releaseTypes;
            return this;
        }

        public List<String> getMoods() {
            return moods;
        }

        public Album setMoods(List<String> moods) {
            this.moods = moods;
            return this;
        }

        public String getSortName() {
            return sortName;
        }

        public Album setSortName(String sortName) {
            this.sortName = sortName;
            return this;
        }

        public OpenSubsonicItemDate getOriginalReleaseDate() {
            return originalReleaseDate;
        }

        public Album setOriginalReleaseDate(OpenSubsonicItemDate originalReleaseDate) {
            this.originalReleaseDate = originalReleaseDate;
            return this;
        }

        public OpenSubsonicItemDate getReleaseDate() {
            return releaseDate;
        }

        public Album setReleaseDate(OpenSubsonicItemDate releaseDate) {
            this.releaseDate = releaseDate;
            return this;
        }

        public boolean isCompilation() {
            return isCompilation;
        }

        public Album setCompilation(boolean compilation) {
            isCompilation = compilation;
            return this;
        }

        public String getExplicitStatus() {
            return explicitStatus;
        }

        public Album setExplicitStatus(String explicitStatus) {
            this.explicitStatus = explicitStatus;
            return this;
        }

        public List<OpenSubsonicDiscTitle> getDiscTitles() {
            return discTitles;
        }

        public Album setDiscTitles(List<OpenSubsonicDiscTitle> discTitles) {
            this.discTitles = discTitles;
            return this;
        }
    }
}
