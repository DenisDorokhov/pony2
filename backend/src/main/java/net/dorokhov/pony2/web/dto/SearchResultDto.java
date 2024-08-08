package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.Genre;
import net.dorokhov.pony2.api.library.domain.Song;

import java.util.ArrayList;
import java.util.List;

public final class SearchResultDto {

    private List<GenreDto> genres = new ArrayList<>();
    private List<ArtistDto> artists = new ArrayList<>();
    private List<AlbumDetailsDto> albumDetails = new ArrayList<>();
    private List<SongDetailsDto> songDetails = new ArrayList<>();

    public List<GenreDto> getGenres() {
        if (genres == null) {
            genres = new ArrayList<>();
        }
        return genres;
    }

    public SearchResultDto setGenres(List<GenreDto> genres) {
        this.genres = genres;
        return this;
    }

    public List<ArtistDto> getArtists() {
        if (artists == null) {
            artists = new ArrayList<>();
        }
        return artists;
    }

    public SearchResultDto setArtists(List<ArtistDto> artists) {
        this.artists = artists;
        return this;
    }

    public List<AlbumDetailsDto> getAlbumDetails() {
        if (albumDetails == null) {
            albumDetails = new ArrayList<>();
        }
        return albumDetails;
    }

    public SearchResultDto setAlbumDetails(List<AlbumDetailsDto> albumDetails) {
        this.albumDetails = albumDetails;
        return this;
    }

    public List<SongDetailsDto> getSongDetails() {
        if (songDetails == null) {
            songDetails = new ArrayList<>();
        }
        return songDetails;
    }

    public SearchResultDto setSongDetails(List<SongDetailsDto> songDetails) {
        this.songDetails = songDetails;
        return this;
    }

    public static SearchResultDto of(List<Genre> genres, List<Artist> artists, List<Album> albums, List<Song> songs, boolean isAdmin) {
        return new SearchResultDto()
                .setGenres(genres.stream()
                        .map(GenreDto::of)
                        .toList())
                .setArtists(artists.stream()
                        .map(ArtistDto::of)
                        .toList())
                .setAlbumDetails(albums.stream()
                        .map(AlbumDetailsDto::of)
                        .toList())
                .setSongDetails(songs.stream()
                        .map(song -> SongDetailsDto.of(song, isAdmin))
                        .toList());
    }
}
