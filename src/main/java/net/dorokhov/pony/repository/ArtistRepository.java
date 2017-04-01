package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;

public interface ArtistRepository extends PagingAndSortingRepository<Artist, Long> {

    long countByArtworkId(Long artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    Artist findByName(String name);

    Page<Artist> findByArtworkId(Long artworkId, Pageable pageable);

    @Modifying
    @Query("UPDATE Artist ar SET ar.artwork = NULL WHERE ar.artwork.id = ?1")
    void clearArtworkByArtworkId(Long artworkId);
}
