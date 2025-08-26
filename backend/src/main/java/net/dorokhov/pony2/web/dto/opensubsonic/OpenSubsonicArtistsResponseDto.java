package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenSubsonicArtistsResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicArtistsResponseDto> {

    private Artists artists;

    public OpenSubsonicArtistsResponseDto(List<OpenSubsonicArtistsResponseDto.Artists.Index.Artist> artists) {
        Map<String, List<Artists.Index.Artist>> letterToArtists = new HashMap<>();
        for (Artists.Index.Artist artist : artists) {
            String letter = artist.getName() != null ? artist.getName().substring(0, 1).toUpperCase() : "U";
            List<Artists.Index.Artist> letterArtists = letterToArtists.computeIfAbsent(letter, k -> new ArrayList<>());
            letterArtists.add(artist);
        }
        this.artists = new Artists().setIndex(letterToArtists.entrySet().stream().map(entry -> new Artists.Index()
                .setName(entry.getKey())
                .setArtist(entry.getValue()))
                .toList());
    }

    public Artists getArtists() {
        return artists;
    }

    public OpenSubsonicArtistsResponseDto setArtists(Artists artists) {
        this.artists = artists;
        return this;
    }

    public static class Artists {

        private List<Index> index;

        public List<Index> getIndex() {
            return index;
        }

        public Artists setIndex(List<Index> index) {
            this.index = index;
            return this;
        }

        public static class Index {

            private String name;
            private List<Artist> artist;

            public String getName() {
                return name;
            }

            public Index setName(String name) {
                this.name = name;
                return this;
            }

            public List<Artist> getArtist() {
                return artist;
            }

            public Index setArtist(List<Artist> artist) {
                this.artist = artist;
                return this;
            }

            public static class Artist {

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

                public String getArtistImageUrl() {
                    return artistImageUrl;
                }

                public Artist setArtistImageUrl(String artistImageUrl) {
                    this.artistImageUrl = artistImageUrl;
                    return this;
                }

                public int getAlbumCount() {
                    return albumCount;
                }

                public Artist setAlbumCount(int albumCount) {
                    this.albumCount = albumCount;
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
            }
        }
    }
}
