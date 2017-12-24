package net.dorokhov.pony.core.library.repository;

import net.dorokhov.pony.api.library.domain.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    long countByArtworkId(@Nullable Long artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    Genre findByName(String name);

    Page<Genre> findByArtworkId(@Nullable Long artworkId, Pageable pageable);

    @Modifying
    @Query("UPDATE Genre g SET g.artwork = NULL WHERE g.artwork.id = ?1")
    void clearArtworkByArtworkId(Long artworkId);
}
