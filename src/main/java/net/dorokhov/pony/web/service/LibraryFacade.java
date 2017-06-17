package net.dorokhov.pony.web.service;

import net.dorokhov.pony.library.domain.ExportBundle;
import net.dorokhov.pony.web.controller.exception.ObjectNotFoundException;
import net.dorokhov.pony.web.domain.*;

import java.util.List;

public interface LibraryFacade {

    List<ArtistDto> getArtists();

    ArtistSongsDto getArtistSongs(Long artistId) throws ObjectNotFoundException;

    List<GenreDto> getGenres();

    GenreSongsDto getGenreSongs(Long genreId, int pageIndex) throws ObjectNotFoundException;

    SearchResultDto search(String query);

    ScanStatusDto getScanStatus();
    
    FileDistribution getSongDistribution(Long songId) throws ObjectNotFoundException;
    FileDistribution getLargeArtworkDistribution(Long artworkId) throws ObjectNotFoundException;
    FileDistribution getSmallArtworkDistribution(Long artworkId) throws ObjectNotFoundException;
    
    ExportBundle exportSong(Long songId) throws ObjectNotFoundException;
    ExportBundle exportAlbum(Long albumId) throws ObjectNotFoundException;
}
