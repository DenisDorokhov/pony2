package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.exception.ArtistNotFoundException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;

import java.util.List;

public interface LibraryService {
    
    List<Artist> getArtists();
    
    Artist getArtistById(Long artistId) throws ArtistNotFoundException;
    
    List<Song> getSongsByIds(List<Long> ids) throws SongNotFoundException;
}
