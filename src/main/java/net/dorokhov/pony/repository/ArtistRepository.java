package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;

public interface ArtistRepository extends PagingAndSortingRepository<Artist, Long> {

    long countByArtworkId(Long aStoredFileId);

    long countByCreationDateGreaterThan(Date aDate);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(Date aCreationDate, Date aUpdateDate);

    Artist findByName(String aName);

    Page<Artist> findByArtworkId(Long aStoredFileId, Pageable aPageable);

    @Modifying
    @Query("UPDATE Artist ar SET ar.artwork = NULL WHERE ar.artwork.id = ?1")
    void clearArtworkByArtworkId(Long aStoredFileId);
}
