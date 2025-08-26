package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.ArrayList;
import java.util.List;

public class OpenSubsonicArtistResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicArtistResponseDto> {

    private Artist artist;

    public Artist getArtist() {
        return artist;
    }

    public OpenSubsonicArtistResponseDto setArtist(Artist artist) {
        this.artist = artist;
        return this;
    }

    public static class Artist {

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

        public Artist setId(String id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Artist setName(String name) {
            this.name = name;
            return this;
        }

        public String getCoverArt() {
            return coverArt;
        }

        public Artist setCoverArt(String coverArt) {
            this.coverArt = coverArt;
            return this;
        }

        public int getAlbumCount() {
            return albumCount;
        }

        public Artist setAlbumCount(int albumCount) {
            this.albumCount = albumCount;
            return this;
        }

        public int getUserRating() {
            return userRating;
        }

        public Artist setUserRating(int userRating) {
            this.userRating = userRating;
            return this;
        }

        public String getArtistImageUrl() {
            return artistImageUrl;
        }

        public Artist setArtistImageUrl(String artistImageUrl) {
            this.artistImageUrl = artistImageUrl;
            return this;
        }

        public String getStarred() {
            return starred;
        }

        public Artist setStarred(String starred) {
            this.starred = starred;
            return this;
        }

        public String getMusicBrainzId() {
            return musicBrainzId;
        }

        public Artist setMusicBrainzId(String musicBrainzId) {
            this.musicBrainzId = musicBrainzId;
            return this;
        }

        public String getSortName() {
            return sortName;
        }

        public Artist setSortName(String sortName) {
            this.sortName = sortName;
            return this;
        }

        public List<String> getRoles() {
            return roles;
        }

        public Artist setRoles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public List<Album> getAlbum() {
            return album;
        }

        public Artist setAlbum(List<Album> album) {
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
            private List<RecordLabel> recordLabels;
            private String musicBrainzId;
            private List<ItemGenre> genres;
            private List<ArtistID3> artists;
            private String displayArtist;
            private List<String> releaseTypes;
            private List<String> moods;
            private String sortName;
            private ItemDate originalReleaseDate;
            private ItemDate releaseDate;
            private boolean isCompilation;
            private String explicitStatus;
            private List<DiscTitle> discTitles;

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

            public List<RecordLabel> getRecordLabels() {
                return recordLabels;
            }

            public Album setRecordLabels(List<RecordLabel> recordLabels) {
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

            public List<ItemGenre> getGenres() {
                return genres;
            }

            public Album setGenres(List<ItemGenre> genres) {
                this.genres = genres;
                return this;
            }

            public List<ArtistID3> getArtists() {
                return artists;
            }

            public Album setArtists(List<ArtistID3> artists) {
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

            public ItemDate getOriginalReleaseDate() {
                return originalReleaseDate;
            }

            public Album setOriginalReleaseDate(ItemDate originalReleaseDate) {
                this.originalReleaseDate = originalReleaseDate;
                return this;
            }

            public ItemDate getReleaseDate() {
                return releaseDate;
            }

            public Album setReleaseDate(ItemDate releaseDate) {
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

            public List<DiscTitle> getDiscTitles() {
                return discTitles;
            }

            public Album setDiscTitles(List<DiscTitle> discTitles) {
                this.discTitles = discTitles;
                return this;
            }

            public static class RecordLabel {

                private String name;

                public String getName() {
                    return name;
                }

                public RecordLabel setName(String name) {
                    this.name = name;
                    return this;
                }
            }

            public static class ItemGenre {

                private String name;

                public String getName() {
                    return name;
                }

                public ItemGenre setName(String name) {
                    this.name = name;
                    return this;
                }
            }

            public static class ArtistID3 {

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

                public ArtistID3 setId(String id) {
                    this.id = id;
                    return this;
                }

                public String getName() {
                    return name;
                }

                public ArtistID3 setName(String name) {
                    this.name = name;
                    return this;
                }

                public String getCoverArt() {
                    return coverArt;
                }

                public ArtistID3 setCoverArt(String coverArt) {
                    this.coverArt = coverArt;
                    return this;
                }

                public String getArtistImageUrl() {
                    return artistImageUrl;
                }

                public ArtistID3 setArtistImageUrl(String artistImageUrl) {
                    this.artistImageUrl = artistImageUrl;
                    return this;
                }

                public int getAlbumCount() {
                    return albumCount;
                }

                public ArtistID3 setAlbumCount(int albumCount) {
                    this.albumCount = albumCount;
                    return this;
                }

                public String getStarred() {
                    return starred;
                }

                public ArtistID3 setStarred(String starred) {
                    this.starred = starred;
                    return this;
                }

                public String getMusicBrainzId() {
                    return musicBrainzId;
                }

                public ArtistID3 setMusicBrainzId(String musicBrainzId) {
                    this.musicBrainzId = musicBrainzId;
                    return this;
                }

                public String getSortName() {
                    return sortName;
                }

                public ArtistID3 setSortName(String sortName) {
                    this.sortName = sortName;
                    return this;
                }

                public List<String> getRoles() {
                    return roles;
                }

                public ArtistID3 setRoles(List<String> roles) {
                    this.roles = roles;
                    return this;
                }
            }

            public static class ItemDate {

                private int year;
                private int month;
                private int day;

                public int getYear() {
                    return year;
                }

                public ItemDate setYear(int year) {
                    this.year = year;
                    return this;
                }

                public int getMonth() {
                    return month;
                }

                public ItemDate setMonth(int month) {
                    this.month = month;
                    return this;
                }

                public int getDay() {
                    return day;
                }

                public ItemDate setDay(int day) {
                    this.day = day;
                    return this;
                }
            }

            public static class DiscTitle {

                private int disc;
                private String title;

                public int getDisc() {
                    return disc;
                }

                public DiscTitle setDisc(int disc) {
                    this.disc = disc;
                    return this;
                }

                public String getTitle() {
                    return title;
                }

                public DiscTitle setTitle(String title) {
                    this.title = title;
                    return this;
                }
            }
        }
    }
}
