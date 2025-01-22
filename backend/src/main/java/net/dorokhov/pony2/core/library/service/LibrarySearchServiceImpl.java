package net.dorokhov.pony2.core.library.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.LibrarySearchService;
import net.dorokhov.pony2.common.SearchTermUtils.Term;
import net.dorokhov.pony2.core.library.service.search.EnglishToRussianLayoutMappingRegistry;
import net.dorokhov.pony2.core.library.service.search.RussianToEnglishLayoutMappingRegistry;
import net.dorokhov.pony2.core.library.service.search.TransliterationMappingRegistry;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesStep;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

import static net.dorokhov.pony2.common.SearchTermUtils.extractTerms;
import static net.dorokhov.pony2.core.library.LibraryConfig.LIBRARY_SEARCH_INDEX_REBUILD_EXECUTOR;

@Service
public class LibrarySearchServiceImpl implements LibrarySearchService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EntityManager entityManager;
    private final EntityManagerFactory entityManagerFactory;
    private final Executor executor;

    // ReentrantLock doesn't fit here, because we release in different thread.
    private final Semaphore reIndexSemaphore = new Semaphore(1);

    public LibrarySearchServiceImpl(
            EntityManager entityManager,
            EntityManagerFactory entityManagerFactory,
            @Qualifier(LIBRARY_SEARCH_INDEX_REBUILD_EXECUTOR) Executor executor
    ) {
        this.entityManager = entityManager;
        this.entityManagerFactory = entityManagerFactory;
        this.executor = executor;
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
        SearchSession session = Search.session(entityManager);
        List<Term> terms = extractTerms(query.getText());
        Set<T> result = new LinkedHashSet<>(search(session, maxResults, clazz, terms, "searchTerms"));
        if (useFallbackQuery && result.size() < maxResults) {
            result.addAll(search(session, maxResults - result.size(), clazz, terms, "fallbackSearchTerms"));
        }
        if (result.isEmpty()) {
            List<Term> englishToRussianTerms = extractTerms(englishToRussianLayout(query.getText()));
            result.addAll(search(session, maxResults, clazz, englishToRussianTerms, "searchTerms"));
            if (useFallbackQuery && result.size() < maxResults) {
                result.addAll(search(session, maxResults - result.size(), clazz, englishToRussianTerms, "fallbackSearchTerms"));
            }
        }
        if (result.isEmpty()) {
            List<Term> russianToEnglishTerms = extractTerms(russianToEnglishLayout(query.getText()));
            result.addAll(search(session, maxResults, clazz, russianToEnglishTerms, "searchTerms"));
            if (useFallbackQuery && result.size() < maxResults) {
                result.addAll(search(session, maxResults - result.size(), clazz, russianToEnglishTerms, "fallbackSearchTerms"));
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

    private <T extends Comparable<T>> List<T> search(SearchSession session, int maxResults, Class<T> clazz, List<Term> terms, String field) {
        return session.search(clazz)
                .where((f, root) -> {
                    for (Term term : terms) {
                        if (!term.getSubWords().isEmpty()) {
                            SimpleBooleanPredicateClausesStep<?> subTermWildcards = f.and();
                            for (String subWord : term.getSubWords()) {
                                subTermWildcards.add(f.wildcard()
                                        .field(field)
                                        .matching(normalizeTerm(subWord) + "*"));
                            }
                            SimpleBooleanPredicateClausesStep<?> rootOr = f.or();
                            rootOr.add(f.wildcard()
                                    .field(field)
                                    .matching(normalizeTerm(term.value()) + "*"));
                            rootOr.add(subTermWildcards);
                            term.getCombinedSubWords().ifPresent(combinedSubWords -> rootOr.add(f.wildcard()
                                    .field(field)
                                    .matching(normalizeTerm(combinedSubWords) + "*")));
                            root.add(rootOr);
                        }
                    }
                })
                .fetch(maxResults)
                .hits();
    }

    private String normalizeTerm(String term) {
        String result = term;
        Map<String, String> mapping = TransliterationMappingRegistry.mapping();
        for (String from : mapping.keySet()) {
            result = result.replace(from, mapping.get(from));
        }
        return result;
    }

    @Override
    public void reIndexAsync() {
        if (!reIndexSemaphore.tryAcquire()) {
            throw new IllegalStateException("Concurrent re-building of library search index.");
        }
        executor.execute(() -> {
            try {
                logger.info("Re-building index...");
                try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
                    Search.session(entityManager).massIndexer().startAndWait();
                    logger.info("Re-building library search index done.");
                } catch (InterruptedException e) {
                    logger.error("Re-building library search index has been interrupted.", e);
                    throw new RuntimeException(e);
                }
            } finally {
                reIndexSemaphore.release();
            }
        });
    }
}
