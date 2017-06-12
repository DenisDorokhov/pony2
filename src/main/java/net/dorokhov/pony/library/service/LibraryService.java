package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.ArtworkFiles;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.exception.ObjectNotFoundException;

import java.util.List;

public interface LibraryService {

    List<Artist> getArtists();

    Artist getArtistById(Long artistId) throws ObjectNotFoundException;

    Song getSongById(Long id) throws ObjectNotFoundException;

    ArtworkFiles getArtworkFilesById(Long id) throws ObjectNotFoundException;

    List<Song> getSongsByIds(List<Long> ids) throws ObjectNotFoundException;
}
