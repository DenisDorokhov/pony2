package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.api.library.domain.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;

public interface GenreRepository extends JpaRepository<Genre, String> {

    long countByArtworkId(@Nullable String artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    Genre findByName(String name);

    Page<Genre> findByArtworkId(@Nullable String artworkId, Pageable pageable);

    @Modifying
    @Query("UPDATE Genre g SET g.artwork = NULL WHERE g.artwork.id = ?1")
    void clearArtworkByArtworkId(String artworkId);
}
