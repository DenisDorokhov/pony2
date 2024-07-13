package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.api.library.domain.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

public interface AlbumRepository extends JpaRepository<Album, String> {

    long countByArtworkId(@Nullable String artworkId);

    long countByArtistId(String artistId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    long countByArtistIdAndArtworkNotNull(String artistId);

    Album findByArtistIdAndName(String artistId, @Nullable String name);

    Page<Album> findByArtistIdAndArtworkNotNull(String artistId, Pageable pageable);

    Page<Album> findByArtworkId(@Nullable String artworkId, Pageable pageable);

    @Modifying
    @Query("UPDATE Album al SET al.artwork = NULL WHERE al.artwork.id = ?1")
    void clearArtworkByArtworkId(String artworkId);
}
