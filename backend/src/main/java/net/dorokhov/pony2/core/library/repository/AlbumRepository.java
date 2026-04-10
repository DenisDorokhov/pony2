package net.dorokhov.pony2.core.library.repository;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.stream.Stream;

public interface AlbumRepository extends JpaRepository<Album, String> {

    long countByArtworkId(@Nullable String artworkId);

    long countByArtistId(String artistId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    long countByArtistIdAndArtworkNotNull(String artistId);

    Album findByArtistIdAndName(String artistId, @Nullable String name);

    Page<Album> findByArtistIdAndArtworkNotNull(String artistId, Pageable pageable);

    Page<Album> findByArtworkId(@Nullable String artworkId, Pageable pageable);

    @Query("SELECT al FROM Album al WHERE al.artwork.id = ?1")
    Stream<Album> streamByArtworkId(String artworkId);

    @Modifying
    @Query("UPDATE Album al SET al.artwork = NULL WHERE al.artwork.id = ?1")
    void clearArtworkByArtworkId(String artworkId);
}
