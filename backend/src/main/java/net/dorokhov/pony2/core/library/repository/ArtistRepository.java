package net.dorokhov.pony2.core.library.repository;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public interface ArtistRepository extends JpaRepository<Artist, String> {

    @Query("SELECT id FROM Artist")
    List<String> findAllIds();

    long countByArtworkId(@Nullable String artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    Artist findByName(@Nullable String name);

    Page<Artist> findByArtworkId(@Nullable String artworkId, Pageable pageable);

    @Query("SELECT ar FROM Artist ar WHERE ar.artwork.id = ?1")
    Stream<Artist> streamByArtworkId(String artworkId);

    @Modifying
    @Query("UPDATE Artist ar SET ar.artwork = NULL WHERE ar.artwork.id = ?1")
    void clearArtworkByArtworkId(String artworkId);
}
