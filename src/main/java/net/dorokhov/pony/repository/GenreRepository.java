package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;

public interface GenreRepository extends PagingAndSortingRepository<Genre, Long> {

    long countByArtworkId(Long artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    Genre findByName(String name);

    Page<Genre> findByArtworkId(Long artworkId, Pageable pageable);

    @Modifying
    @Query("UPDATE Genre g SET g.artwork = NULL WHERE g.artwork.id = ?1")
    void clearArtworkByArtworkId(Long artworkId);
}
