package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;

import java.util.List;

public interface LibraryFacade {
    
    List<SongDetailsDto> getSongs(List<String> songIds);

    List<ArtistDto> getArtists();

    ArtistSongsDto getArtistSongs(String artistId) throws ObjectNotFoundException;

    List<GenreDto> getGenres();

    GenreSongsPageDto getGenreSongs(String genreId, int pageIndex) throws ObjectNotFoundException;

    SearchResultDto search(String query);

    List<SongDetailsDto> getRandomSongs(int count);

    List<SongDetailsDto> getRandomSongsByAlbumId(String albumId, int count);

    List<SongDetailsDto> getRandomSongsByArtistId(String artistId, int count);

    List<SongDetailsDto> getRandomSongsByGenreId(String genreId, int count);
}
