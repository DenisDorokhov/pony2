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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class LibrarySearchServiceImpl implements LibrarySearchService {

    private final EntityManager entityManager;

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
        String notUncodeLetterAndDigitRegex = "[^\\p{L}\\s0-9']";
        String[] normalWords = query.getText()
                .trim()
                .replaceAll(notUncodeLetterAndDigitRegex, " ")
                .toLowerCase()
                .split("\\s+");
        List<T> result = new ArrayList<>(search(maxResults, clazz, normalWords, "searchTerms"));
        if (useFallbackQuery && result.isEmpty()) {
            result.addAll(search(maxResults, clazz, normalWords, "fallbackSearchTerms"));
        }
        if (result.isEmpty()) {
            String[] englishToRussianLayoutWords = englishToRussianLayout(query.getText())
                    .trim()
                    .replaceAll(notUncodeLetterAndDigitRegex, " ")
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
                    .replaceAll(notUncodeLetterAndDigitRegex, " ")
                    .toLowerCase()
                    .split("\\s+");
            result.addAll(search(maxResults, clazz, russianToEnglishLayoutWords, "searchTerms"));
            if (useFallbackQuery && result.isEmpty()) {
                result.addAll(search(maxResults, clazz, russianToEnglishLayoutWords, "fallbackSearchTerms"));
            }
        }
        Collections.sort(result);
        return result;
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
}
