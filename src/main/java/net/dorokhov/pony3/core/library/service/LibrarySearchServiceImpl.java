package net.dorokhov.pony3.core.library.service;

import net.dorokhov.pony3.api.library.domain.Album;
import net.dorokhov.pony3.api.library.domain.Artist;
import net.dorokhov.pony3.api.library.domain.Genre;
import net.dorokhov.pony3.api.library.domain.Song;
import net.dorokhov.pony3.api.library.domain.LibrarySearchQuery;
import net.dorokhov.pony3.api.library.service.LibrarySearchService;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.Collections;
import java.util.List;

@Service
public class LibrarySearchServiceImpl implements LibrarySearchService {

    private final EntityManager entityManager;

    public LibrarySearchServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Genre> searchGenres(LibrarySearchQuery query, int maxResults) {
        return search(query, maxResults, Genre.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> searchArtists(LibrarySearchQuery query, int maxResults) {
        return search(query, maxResults, Artist.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Album> searchAlbums(LibrarySearchQuery query, int maxResults) {
        return search(query, maxResults, Album.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> searchSongs(LibrarySearchQuery query, int maxResults) {
        return search(query, maxResults, Song.class);
    }

    private <T extends Comparable<T>> List<T> search(LibrarySearchQuery query, int maxResults, Class<T> clazz) {
        String[] words = query.getText()
                .trim()
                .toLowerCase()
                .split("\\s+");
        List<T> result = Search.session(entityManager).search(clazz)
                .where((f, root) -> {
                    for (String word : words) {
                        root.add(f.wildcard()
                                .field("searchTerms")
                                .matching(word + "*"));
                    }
                })
                .fetch(maxResults)
                .hits();
        Collections.sort(result);
        return result;
    }
}
