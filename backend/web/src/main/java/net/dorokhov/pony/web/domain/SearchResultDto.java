package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Album;
import net.dorokhov.pony.api.library.domain.Artist;
import net.dorokhov.pony.api.library.domain.Genre;
import net.dorokhov.pony.api.library.domain.Song;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public final class SearchResultDto {

    private final List<GenreDto> genres;
    private final List<ArtistDto> artists;
    private final List<AlbumDetailsDto> albumDetails;
    private final List<SongDetailsDto> songDetails;

    private SearchResultDto(List<GenreDto> genres,
                            List<ArtistDto> artists,
                            List<AlbumDetailsDto> albumDetails,
                            List<SongDetailsDto> songDetails) {
        this.genres = unmodifiableList(genres);
        this.artists = unmodifiableList(artists);
        this.albumDetails = unmodifiableList(albumDetails);
        this.songDetails = unmodifiableList(songDetails);
    }

    public List<GenreDto> getGenres() {
        return genres;
    }

    public List<ArtistDto> getArtists() {
        return artists;
    }

    public List<AlbumDetailsDto> getAlbumDetails() {
        return albumDetails;
    }

    public List<SongDetailsDto> getSongDetails() {
        return songDetails;
    }

    public static SearchResultDto of(List<Genre> genres, List<Artist> artists, List<Album> albums, List<Song> songs) {
        return new SearchResultDto(genres.stream().map(GenreDto::of).collect(Collectors.toList()),
                artists.stream().map(ArtistDto::of).collect(Collectors.toList()),
                albums.stream().map(AlbumDetailsDto::of).collect(Collectors.toList()),
                songs.stream().map(SongDetailsDto::of).collect(Collectors.toList()));
    }
}
