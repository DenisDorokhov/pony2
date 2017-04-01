package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.Artwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ArtworkRepository extends PagingAndSortingRepository<Artwork, Long> {

    long countByTag(String tag);

    long countByTagAndDateGreaterThan(String tag, LocalDateTime date);

    @Query("SELECT SUM(f.largeImageSize) FROM Artwork f WHERE f.tag = ?1")
    Long sumSizeByTag(String tag);

    Page<Artwork> findByTag(String tag, Pageable pageable);

    List<Artwork> findByChecksum(String checksum);

    Artwork findByTagAndChecksum(String tag, String checksum);
}
