package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;

public interface GenreRepository extends PagingAndSortingRepository<Genre, Long> {

    long countByArtworkId(Long aStoredFileId);

    long countByCreationDateGreaterThan(Date aDate);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(Date aCreationDate, Date aUpdateDate);

    Genre findByName(String aName);

    Page<Genre> findByArtworkId(Long aStoredFileId, Pageable aPageable);

    @Modifying
    @Query("UPDATE Genre g SET g.artwork = NULL WHERE g.artwork.id = ?1")
    void clearArtworkByArtworkId(Long aStoredFileId);
}
