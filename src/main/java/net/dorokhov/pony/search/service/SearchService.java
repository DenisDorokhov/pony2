package net.dorokhov.pony.search.service;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.domain.Song;

import java.util.List;

public interface SearchService {

    List<Genre> searchGenres(String query, int maxResults);
    
    List<Artist> searchArtists(String query, int maxResults);
    
    List<Album> searchAlbums(String query, int maxResults);
    
    List<Song> searchSongs(String query, int maxResults);
}
