package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.ArtistGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArtistGenreRepository extends JpaRepository<ArtistGenre, String> {
    @Query("SELECT artist.id FROM ArtistGenre WHERE genre.id IN ?1")
    List<String> findAllArtistIdsByGenreIdIn(List<String> genreIds);
    void deleteByArtistId(String genreId);
    void deleteByGenreId(String genreId);
}
