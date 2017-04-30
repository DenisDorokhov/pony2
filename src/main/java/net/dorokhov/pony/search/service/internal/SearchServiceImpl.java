package net.dorokhov.pony.search.service.internal;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.search.service.SearchService;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

@Service
public class SearchServiceImpl implements SearchService {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final EntityManager entityManager;

    public SearchServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Genre> searchGenres(String query, int maxResults) {
        return search(query, maxResults, Genre.class, "name");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> searchArtists(String query, int maxResults) {
        return search(query, maxResults, Artist.class, "name");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Album> searchAlbums(String query, int maxResults) {
        return search(query, maxResults, Album.class, "searchTerms");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> searchSongs(String query, int maxResults) {
        return search(query, maxResults, Song.class, "searchTerms");
    }
    
    @SuppressWarnings("unchecked")
    private <T> List<T> search(String query, int maxResults, Class<T> clazz, String searchField) {

        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

        QueryBuilder searchQuery = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(clazz).get();
        BooleanQuery.Builder luceneQuery = new BooleanQuery.Builder();
        for (String word : query.trim().toLowerCase().split("\\s+")) {
            Query wordQuery = searchQuery
                    .keyword().wildcard()
                    .onField(searchField)
                    .matching(word + "*")
                    .createQuery();
            luceneQuery.add(
                    wordQuery,
                    StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word) ? SHOULD : MUST);
        }
        
        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery.build(), clazz);
        jpaQuery.setMaxResults(maxResults);
        return jpaQuery.getResultList();
    }
}
