package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.api.library.domain.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    long countByArtworkId(@Nullable Long artworkId);

    long countByArtistId(Long artistId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    long countByArtistIdAndArtworkNotNull(Long artistId);

    Album findByArtistIdAndName(Long artistId, @Nullable String name);

    Page<Album> findByArtistIdAndArtworkNotNull(Long artistId, Pageable pageable);

    Page<Album> findByArtworkId(@Nullable Long artworkId, Pageable pageable);

    @Modifying
    @Query("UPDATE Album al SET al.artwork = NULL WHERE al.artwork.id = ?1")
    void clearArtworkByArtworkId(Long artworkId);
}
