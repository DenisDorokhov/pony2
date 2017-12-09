package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.domain.LibrarySearchQuery;

import java.util.List;

public interface LibrarySearchService {

    List<Genre> searchGenres(LibrarySearchQuery query, int maxResults);
    
    List<Artist> searchArtists(LibrarySearchQuery query, int maxResults);
    
    List<Album> searchAlbums(LibrarySearchQuery query, int maxResults);
    
    List<Song> searchSongs(LibrarySearchQuery query, int maxResults);
}
