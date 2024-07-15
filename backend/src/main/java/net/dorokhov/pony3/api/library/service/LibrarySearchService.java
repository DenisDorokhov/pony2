package net.dorokhov.pony3.api.library.service;

import net.dorokhov.pony3.api.library.domain.Album;
import net.dorokhov.pony3.api.library.domain.Artist;
import net.dorokhov.pony3.api.library.domain.Genre;
import net.dorokhov.pony3.api.library.domain.Song;
import net.dorokhov.pony3.api.library.domain.LibrarySearchQuery;

import java.util.List;

public interface LibrarySearchService {

    List<Genre> searchGenres(LibrarySearchQuery query, int maxResults);
    
    List<Artist> searchArtists(LibrarySearchQuery query, int maxResults);
    
    List<Album> searchAlbums(LibrarySearchQuery query, int maxResults);
    
    List<Song> searchSongs(LibrarySearchQuery query, int maxResults);
}
