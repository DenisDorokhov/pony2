package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.Genre;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.domain.LibrarySearchQuery;
import net.dorokhov.pony2.api.library.service.LibrarySearchService;
import net.dorokhov.pony2.core.library.service.search.EnglishToRussianLayoutMappingRegistry;
import net.dorokhov.pony2.core.library.service.search.RussianToEnglishLayoutMappingRegistry;
import net.dorokhov.pony2.core.library.service.search.TransliterationMappingRegistry;
import org.hibernate.search.mapper.orm.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LibrarySearchServiceImpl implements LibrarySearchService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EntityManager entityManager;

    private final Lock reIndexLock = new ReentrantLock();

    public LibrarySearchServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Genre> searchGenres(LibrarySearchQuery query, int maxResults) {
        return search(query, maxResults, Genre.class, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> searchArtists(LibrarySearchQuery query, int maxResults) {
        return search(query, maxResults, Artist.class, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Album> searchAlbums(LibrarySearchQuery query, int maxResults) {
        return search(query, maxResults, Album.class, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> searchSongs(LibrarySearchQuery query, int maxResults) {
        return search(query, maxResults, Song.class, true);
    }

    private <T extends Comparable<T>> List<T> search(LibrarySearchQuery query, int maxResults, Class<T> clazz, boolean useFallbackQuery) {
        String nonUnicodeLetterAndDigitRegex = "[^\\p{L}\\s\\p{N}]";
        String[] normalWords = query.getText()
                .trim()
                .replaceAll(nonUnicodeLetterAndDigitRegex, " ")
                .toLowerCase()
                .split("\\s+");
        Set<T> result = new LinkedHashSet<>(search(maxResults, clazz, normalWords, "searchTerms"));
        if (useFallbackQuery && result.size() < maxResults) {
            result.addAll(search(maxResults - result.size(), clazz, normalWords, "fallbackSearchTerms"));
        }
        if (result.isEmpty()) {
            String[] englishToRussianLayoutWords = englishToRussianLayout(query.getText())
                    .trim()
                    .replaceAll(nonUnicodeLetterAndDigitRegex, " ")
                    .toLowerCase()
                    .split("\\s+");
            result.addAll(search(maxResults, clazz, englishToRussianLayoutWords, "searchTerms"));
            if (useFallbackQuery && result.isEmpty()) {
                result.addAll(search(maxResults, clazz, englishToRussianLayoutWords, "fallbackSearchTerms"));
            }
        }
        if (result.isEmpty()) {
            String[] russianToEnglishLayoutWords = russianToEnglishLayout(query.getText())
                    .trim()
                    .replaceAll(nonUnicodeLetterAndDigitRegex, " ")
                    .toLowerCase()
                    .split("\\s+");
            result.addAll(search(maxResults, clazz, russianToEnglishLayoutWords, "searchTerms"));
            if (useFallbackQuery && result.isEmpty()) {
                result.addAll(search(maxResults, clazz, russianToEnglishLayoutWords, "fallbackSearchTerms"));
            }
        }
        return result.stream()
                .sorted()
                .toList();
    }

    private String englishToRussianLayout(String query) {
        Map<String, String> mapping = EnglishToRussianLayoutMappingRegistry.mapping();
        for (String from : mapping.keySet()) {
            query = query.replace(from, mapping.get(from));
        }
        return query;
    }

    private String russianToEnglishLayout(String query) {
        Map<String, String> mapping = RussianToEnglishLayoutMappingRegistry.mapping();
        for (String from : mapping.keySet()) {
            query = query.replace(from, mapping.get(from));
        }
        return query;
    }

    private <T extends Comparable<T>> List<T> search(int maxResults, Class<T> clazz, String[] words, String field) {
        return Search.session(entityManager).search(clazz)
                .where((f, root) -> {
                    for (String word : words) {
                        root.add(f.wildcard()
                                .field(field)
                                .matching(normalizeWord(word) + "*"));
                    }
                })
                .fetch(maxResults)
                .hits();
    }

    private String normalizeWord(String word) {
        Map<String, String> mapping = TransliterationMappingRegistry.mapping();
        for (String from : mapping.keySet()) {
            word = word.replace(from, mapping.get(from));
        }
        return word;
    }

    @Override
    public void reIndex() {
        if (!reIndexLock.tryLock()) {
            throw new IllegalStateException("Concurrent re-building of library search index.");
        }
        try {
            logger.info("Re-building index...");
            try {
                Search.session(entityManager).massIndexer().startAndWait();
                logger.info("Re-building library search index done.");
            } catch (InterruptedException e) {
                logger.error("Mass indexer has been interrupted.", e);
                throw new RuntimeException(e);
            }
        } finally {
            reIndexLock.unlock();
        }
    }
}
