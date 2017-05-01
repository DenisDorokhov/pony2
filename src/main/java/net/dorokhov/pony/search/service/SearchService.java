package net.dorokhov.pony.search.service;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.search.domain.SearchQuery;

import java.util.List;

public interface SearchService {

    List<Genre> searchGenres(SearchQuery query, int maxResults);
    
    List<Artist> searchArtists(SearchQuery query, int maxResults);
    
    List<Album> searchAlbums(SearchQuery query, int maxResults);
    
    List<Song> searchSongs(SearchQuery query, int maxResults);
}
