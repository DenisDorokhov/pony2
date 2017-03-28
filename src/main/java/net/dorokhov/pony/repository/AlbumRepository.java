package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;

public interface AlbumRepository extends PagingAndSortingRepository<Album, Long> {

    long countByArtworkId(Long aStoredFileId);

    long countByArtistId(Long aArtistId);

    long countByCreationDateGreaterThan(LocalDateTime aDate);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime aCreationDate, LocalDateTime aUpdateDate);

    long countByArtistIdAndArtworkNotNull(Long aArtistId);

    Album findByArtistIdAndName(Long aArtistId, String aName);

    Page<Album> findByArtistIdAndArtworkNotNull(Long aArtistId, Pageable aPageable);

    Page<Album> findByArtworkId(Long aStoredFileId, Pageable aPageable);

    @Modifying
    @Query("UPDATE Album al SET al.artwork = NULL WHERE al.artwork.id = ?1")
    void clearArtworkByArtworkId(Long aStoredFileId);
}
