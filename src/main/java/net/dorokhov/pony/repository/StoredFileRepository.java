package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.StoredFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;
import java.util.List;

public interface StoredFileRepository extends PagingAndSortingRepository<StoredFile, Long> {

    long countByTag(String aTag);

    long countByTagAndDateGreaterThan(String aTag, Date aDate);

    @Query("SELECT SUM(f.size) FROM StoredFile f WHERE f.tag = ?1")
    Long sumSizeByTag(String aTag);

    Page<StoredFile> findByTag(String aTag, Pageable aPageable);

    List<StoredFile> findByChecksum(String aChecksum);

    StoredFile findByTagAndChecksum(String aTag, String aChecksum);
}
