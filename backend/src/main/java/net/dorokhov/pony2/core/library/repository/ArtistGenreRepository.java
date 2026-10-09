package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.ArtistGenre;
import net.dorokhov.pony2.api.library.domain.Genre;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArtistGenreRepository extends JpaRepository<ArtistGenre, String> {

    @Query("SELECT artist.id FROM ArtistGenre WHERE genre.id IN ?1")
    List<String> findAllArtistIdsByGenreIdIn(List<String> genreIds);

    @Query("""
            SELECT g FROM ArtistGenre link JOIN link.genre g
            WHERE link.artist.id = :artistId AND g.id > :afterGenreId
            ORDER BY g.id
            """)
    List<Genre> findGenresForEvaluation(String artistId, String afterGenreId, Pageable pageable);

    void deleteByArtistId(String genreId);
    void deleteByGenreId(String genreId);
}
