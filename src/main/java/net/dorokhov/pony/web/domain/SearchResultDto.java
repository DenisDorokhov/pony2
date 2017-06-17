package net.dorokhov.pony.web.domain;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.domain.Song;

import java.util.List;
import java.util.stream.Collectors;

public final class SearchResultDto {
    
    private final List<GenreDto> genres;
    private final List<ArtistDto> artists;
    private final List<AlbumDetailsDto> albums;
    private final List<SongDetailsDto> songs;

    public SearchResultDto(List<GenreDto> genres, 
                           List<ArtistDto> artists, 
                           List<AlbumDetailsDto> albums, 
                           List<SongDetailsDto> songs) {
        this.genres = ImmutableList.copyOf(genres);
        this.artists = ImmutableList.copyOf(artists);
        this.albums = ImmutableList.copyOf(albums);
        this.songs = ImmutableList.copyOf(songs);
    }

    public List<GenreDto> getGenres() {
        return genres;
    }

    public List<ArtistDto> getArtists() {
        return artists;
    }

    public List<AlbumDetailsDto> getAlbums() {
        return albums;
    }

    public List<SongDetailsDto> getSongs() {
        return songs;
    }
    
    public static SearchResultDto of(List<Genre> genres, List<Artist> artists, List<Album> albums, List<Song> songs) {
        return new SearchResultDto(genres.stream().map(GenreDto::of).collect(Collectors.toList()),
                artists.stream().map(ArtistDto::of).collect(Collectors.toList()),
                albums.stream().map(AlbumDetailsDto::of).collect(Collectors.toList()),
                songs.stream().map(SongDetailsDto::of).collect(Collectors.toList()));
    }
}
