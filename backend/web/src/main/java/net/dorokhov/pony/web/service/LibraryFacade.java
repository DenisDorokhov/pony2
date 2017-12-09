package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;

import java.util.List;

public interface LibraryFacade {

    List<ArtistDto> getArtists();

    ArtistSongsDto getArtistSongs(Long artistId) throws ObjectNotFoundException;

    List<GenreDto> getGenres();

    GenreSongsPageDto getGenreSongs(Long genreId, int pageIndex) throws ObjectNotFoundException;

    SearchResultDto search(String query);
}
