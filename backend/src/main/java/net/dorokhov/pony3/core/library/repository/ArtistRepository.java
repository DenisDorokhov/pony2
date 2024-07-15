package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.api.library.domain.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;

public interface ArtistRepository extends JpaRepository<Artist, String> {

    long countByArtworkId(@Nullable String artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    Artist findByName(@Nullable String name);

    Page<Artist> findByArtworkId(@Nullable String artworkId, Pageable pageable);

    @Modifying
    @Query("UPDATE Artist ar SET ar.artwork = NULL WHERE ar.artwork.id = ?1")
    void clearArtworkByArtworkId(String artworkId);
}
