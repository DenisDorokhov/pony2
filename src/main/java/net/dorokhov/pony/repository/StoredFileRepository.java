package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.StoredFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StoredFileRepository extends PagingAndSortingRepository<StoredFile, Long> {

    long countByTag(String tag);

    long countByTagAndDateGreaterThan(String tag, LocalDateTime date);

    @Query("SELECT SUM(f.size) FROM StoredFile f WHERE f.tag = ?1")
    Long sumSizeByTag(String tag);

    Page<StoredFile> findByTag(String tag, Pageable pageable);

    List<StoredFile> findByChecksum(String checksum);

    StoredFile findByTagAndChecksum(String tag, String checksum);
}
