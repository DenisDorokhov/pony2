package net.dorokhov.pony3;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

public final class ScanTestPlan {

    private static class Artwork {

        private final String copyFrom;

        public Artwork(String copyFrom) {
            this.copyFrom = checkNotNull(copyFrom);
        }

        public String getCopyFrom() {
            return copyFrom;
        }
    }

    public static final class FileArtwork extends Artwork {

        private final String copyTo;

        public FileArtwork(String copyFrom, String copyTo) {
            super(copyFrom);
            this.copyTo = checkNotNull(copyTo);
        }

        public String getCopyTo() {
            return copyTo;
        }
    }

    public static final class EmbeddedArtwork extends Artwork {

        @JsonCreator
        public EmbeddedArtwork(String copyFrom) {
            super(copyFrom);
        }
    }

    public static final class SongToGenerate {

        private final String path;
        private final Integer discNumber;
        private final Integer discCount;
        private final Integer trackNumber;
        private final Integer trackCount;
        private final String title;
        private final String artist;
        private final String albumArtist;
        private final String album;
        private final Integer year;
        private final String genre;
        private final EmbeddedArtwork embeddedArtwork;

        public SongToGenerate(
                String path,
                @Nullable Integer discNumber, @Nullable Integer discCount,
                @Nullable Integer trackNumber, @Nullable Integer trackCount,
                @Nullable String title, @Nullable String artist, @Nullable String albumArtist,
                @Nullable String album, @Nullable Integer year, @Nullable String genre,
                @Nullable EmbeddedArtwork embeddedArtwork
        ) {
            this.path = checkNotNull(path);
            this.discNumber = discNumber;
            this.discCount = discCount;
            this.trackNumber = trackNumber;
            this.trackCount = trackCount;
            this.title = title;
            this.artist = artist;
            this.albumArtist = albumArtist;
            this.album = album;
            this.year = year;
            this.genre = genre;
            this.embeddedArtwork = embeddedArtwork;
        }

        public String getPath() {
            return path;
        }

        @Nullable
        public Integer getDiscNumber() {
            return discNumber;
        }

        @Nullable
        public Integer getDiscCount() {
            return discCount;
        }

        @Nullable
        public Integer getTrackNumber() {
            return trackNumber;
        }

        @Nullable
        public Integer getTrackCount() {
            return trackCount;
        }

        @Nullable
        public String getTitle() {
            return title;
        }

        @Nullable
        public String getArtist() {
            return artist;
        }

        @Nullable
        public String getAlbumArtist() {
            return albumArtist;
        }

        @Nullable
        public String getAlbum() {
            return album;
        }

        @Nullable
        public Integer getYear() {
            return year;
        }

        @Nullable
        public String getGenre() {
            return genre;
        }

        @Nullable
        public EmbeddedArtwork getEmbeddedArtwork() {
            return embeddedArtwork;
        }
    }

    public static final class ExpectedData {

        public static final class Result {

            private final Integer processedAudioFileCount;
            private final Integer genreCount;
            private final Integer artistCount;
            private final Integer albumCount;
            private final Integer songCount;
            private final Integer artworkCount;
            private final Integer createdArtistCount;
            private final Integer updatedArtistCount;
            private final Integer deletedArtistCount;
            private final Integer createdAlbumCount;
            private final Integer updatedAlbumCount;
            private final Integer deletedAlbumCount;
            private final Integer createdGenreCount;
            private final Integer updatedGenreCount;
            private final Integer deletedGenreCount;
            private final Integer createdSongCount;
            private final Integer updatedSongCount;
            private final Integer deletedSongCount;
            private final Integer createdArtworkCount;
            private final Integer deletedArtworkCount;

            public Result(
                    Integer processedAudioFileCount,
                    Integer genreCount, Integer artistCount, Integer albumCount, Integer songCount, Integer artworkCount,
                    Integer createdArtistCount, Integer updatedArtistCount, Integer deletedArtistCount,
                    Integer createdAlbumCount, Integer updatedAlbumCount, Integer deletedAlbumCount,
                    Integer createdGenreCount, Integer updatedGenreCount, Integer deletedGenreCount,
                    Integer createdSongCount, Integer updatedSongCount, Integer deletedSongCount,
                    Integer createdArtworkCount, Integer deletedArtworkCount
            ) {
                this.processedAudioFileCount = checkNotNull(processedAudioFileCount);
                this.genreCount = checkNotNull(genreCount);
                this.artistCount = checkNotNull(artistCount);
                this.albumCount = checkNotNull(albumCount);
                this.songCount = checkNotNull(songCount);
                this.artworkCount = checkNotNull(artworkCount);
                this.createdArtistCount = checkNotNull(createdArtistCount);
                this.updatedArtistCount = checkNotNull(updatedArtistCount);
                this.deletedArtistCount = checkNotNull(deletedArtistCount);
                this.createdAlbumCount = checkNotNull(createdAlbumCount);
                this.updatedAlbumCount = checkNotNull(updatedAlbumCount);
                this.deletedAlbumCount = checkNotNull(deletedAlbumCount);
                this.createdGenreCount = checkNotNull(createdGenreCount);
                this.updatedGenreCount = checkNotNull(updatedGenreCount);
                this.deletedGenreCount = checkNotNull(deletedGenreCount);
                this.createdSongCount = checkNotNull(createdSongCount);
                this.updatedSongCount = checkNotNull(updatedSongCount);
                this.deletedSongCount = checkNotNull(deletedSongCount);
                this.createdArtworkCount = checkNotNull(createdArtworkCount);
                this.deletedArtworkCount = checkNotNull(deletedArtworkCount);
            }

            public Integer getProcessedAudioFileCount() {
                return processedAudioFileCount;
            }

            public Integer getGenreCount() {
                return genreCount;
            }

            public Integer getArtistCount() {
                return artistCount;
            }

            public Integer getAlbumCount() {
                return albumCount;
            }

            public Integer getSongCount() {
                return songCount;
            }

            public Integer getArtworkCount() {
                return artworkCount;
            }

            public Integer getCreatedArtistCount() {
                return createdArtistCount;
            }

            public Integer getUpdatedArtistCount() {
                return updatedArtistCount;
            }

            public Integer getDeletedArtistCount() {
                return deletedArtistCount;
            }

            public Integer getCreatedAlbumCount() {
                return createdAlbumCount;
            }

            public Integer getUpdatedAlbumCount() {
                return updatedAlbumCount;
            }

            public Integer getDeletedAlbumCount() {
                return deletedAlbumCount;
            }

            public Integer getCreatedGenreCount() {
                return createdGenreCount;
            }

            public Integer getUpdatedGenreCount() {
                return updatedGenreCount;
            }

            public Integer getDeletedGenreCount() {
                return deletedGenreCount;
            }

            public Integer getCreatedSongCount() {
                return createdSongCount;
            }

            public Integer getUpdatedSongCount() {
                return updatedSongCount;
            }

            public Integer getDeletedSongCount() {
                return deletedSongCount;
            }

            public Integer getCreatedArtworkCount() {
                return createdArtworkCount;
            }

            public Integer getDeletedArtworkCount() {
                return deletedArtworkCount;
            }
        }

        public static final class Genre {

            private final String name;
            private final String artworkPath;
            private final List<String> songPaths;

            public Genre(@Nullable String name, @Nullable String artworkPath, List<String> songPaths) {
                this.name = name;
                this.artworkPath = artworkPath;
                this.songPaths = unmodifiableList(songPaths);
            }

            @Nullable
            public String getName() {
                return name;
            }

            @Nullable
            public String getArtworkPath() {
                return artworkPath;
            }

            public List<String> getSongPaths() {
                return songPaths;
            }
        }

        public static final class Artist {

            public static final class Album {

                private final String name;
                private final Integer year;
                private final String artworkPath;
                private final List<String> songPaths;

                public Album(
                        @Nullable String name, @Nullable Integer year,
                        @Nullable String artworkPath, List<String> songPaths
                ) {
                    this.name = name;
                    this.year = year;
                    this.artworkPath = artworkPath;
                    this.songPaths = unmodifiableList(songPaths);
                }

                @Nullable
                public String getName() {
                    return name;
                }

                @Nullable
                public Integer getYear() {
                    return year;
                }

                @Nullable
                public String getArtworkPath() {
                    return artworkPath;
                }

                public List<String> getSongPaths() {
                    return songPaths;
                }
            }

            private final String name;
            private final String artworkPath;
            private final List<Album> albums;

            public Artist(@Nullable String name, @Nullable String artworkPath, List<Album> albums) {
                this.name = name;
                this.artworkPath = artworkPath;
                this.albums = unmodifiableList(albums);
            }

            @Nullable
            public String getName() {
                return name;
            }

            @Nullable
            public String getArtworkPath() {
                return artworkPath;
            }

            public List<Album> getAlbums() {
                return albums;
            }
        }

        public static final class Song {

            private final String path;
            private final String mimeType;
            private final String fileExtension;
            private final Integer discNumber;
            private final Integer discCount;
            private final Integer trackNumber;
            private final Integer trackCount;
            private final String title;
            private final String artist;
            private final String albumArtist;
            private final String album;
            private final Integer year;
            private final String genre;
            private final String artworkPath;

            public Song(
                    String path, String mimeType, String fileExtension,
                    @Nullable Integer discNumber, @Nullable Integer discCount,
                    @Nullable Integer trackNumber, @Nullable Integer trackCount,
                    @Nullable String title, @Nullable String artist, @Nullable String albumArtist,
                    @Nullable String album, @Nullable Integer year, @Nullable String genre,
                    @Nullable String artworkPath
            ) {
                this.path = checkNotNull(path);
                this.mimeType = checkNotNull(mimeType);
                this.fileExtension = checkNotNull(fileExtension);
                this.discNumber = discNumber;
                this.discCount = discCount;
                this.trackNumber = trackNumber;
                this.trackCount = trackCount;
                this.title = title;
                this.artist = artist;
                this.albumArtist = albumArtist;
                this.album = album;
                this.year = year;
                this.genre = genre;
                this.artworkPath = artworkPath;
            }

            public String getPath() {
                return path;
            }

            public String getMimeType() {
                return mimeType;
            }

            public String getFileExtension() {
                return fileExtension;
            }

            @Nullable
            public Integer getDiscNumber() {
                return discNumber;
            }

            @Nullable
            public Integer getDiscCount() {
                return discCount;
            }

            @Nullable
            public Integer getTrackNumber() {
                return trackNumber;
            }

            @Nullable
            public Integer getTrackCount() {
                return trackCount;
            }

            @Nullable
            public String getTitle() {
                return title;
            }

            @Nullable
            public String getArtist() {
                return artist;
            }

            @Nullable
            public String getAlbumArtist() {
                return albumArtist;
            }

            @Nullable
            public String getAlbum() {
                return album;
            }

            @Nullable
            public Integer getYear() {
                return year;
            }

            @Nullable
            public String getGenre() {
                return genre;
            }

            @Nullable
            public String getArtworkPath() {
                return artworkPath;
            }
        }

        private final Result result;
        private final List<Genre> genres;
        private final List<Artist> artists;
        private final List<Song> songs;

        public ExpectedData(Result result, List<Genre> genres, List<Artist> artists, List<Song> songs) {
            this.result = checkNotNull(result);
            this.genres = unmodifiableList(genres);
            this.artists = unmodifiableList(artists);
            this.songs = unmodifiableList(songs);
        }

        public Result getResult() {
            return result;
        }

        public List<Genre> getGenres() {
            return genres;
        }

        public List<Artist> getArtists() {
            return artists;
        }

        public List<Song> getSongs() {
            return songs;
        }
    }

    private final List<FileArtwork> artworksToGenerate;
    private final List<SongToGenerate> songsToGenerate;
    private final List<String> filePathsToDelete;
    private final ExpectedData expectedData;

    public ScanTestPlan(
            List<FileArtwork> artworksToGenerate, List<SongToGenerate> songsToGenerate,
            List<String> filePathsToDelete, ExpectedData expectedData
    ) {
        this.artworksToGenerate = unmodifiableList(artworksToGenerate);
        this.songsToGenerate = unmodifiableList(songsToGenerate);
        this.filePathsToDelete = unmodifiableList(filePathsToDelete);
        this.expectedData = checkNotNull(expectedData);
    }

    public List<FileArtwork> getArtworksToGenerate() {
        return artworksToGenerate;
    }

    public List<SongToGenerate> getSongsToGenerate() {
        return songsToGenerate;
    }

    public List<String> getFilePathsToDelete() {
        return filePathsToDelete;
    }

    public ExpectedData getExpectedData() {
        return expectedData;
    }
}
