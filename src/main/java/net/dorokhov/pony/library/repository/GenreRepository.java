package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.library.domain.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

public interface GenreRepository extends PagingAndSortingRepository<Genre, Long> {

    long countByArtworkId(@Nullable Long artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    Genre findByName(String name);

    Page<Genre> findByArtworkId(@Nullable Long artworkId, Pageable pageable);

    @Modifying
    @Query("UPDATE Genre g SET g.artwork = NULL WHERE g.artwork.id = ?1")
    void clearArtworkByArtworkId(Long artworkId);
}
