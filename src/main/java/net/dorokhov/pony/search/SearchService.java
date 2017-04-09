package net.dorokhov.pony.search;

import net.dorokhov.pony.entity.Album;
import net.dorokhov.pony.entity.Artist;
import net.dorokhov.pony.entity.Genre;
import net.dorokhov.pony.entity.Song;

import java.util.List;

public interface SearchService {

    void purgeIndex();

    List<Genre> searchGenres(String query, int maxResults);
    List<Artist> searchArtists(String query, int maxResults);
    List<Album> searchAlbums(String query, int maxResults);
    List<Song> searchSongs(String query, int maxResults);
}
