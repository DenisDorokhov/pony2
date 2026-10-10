package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.AlbumDiscovery;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlbumDiscoveryRepository extends JpaRepository<AlbumDiscovery, String> {

    Optional<AlbumDiscovery> findFirstByAlbumIdOrderByCreationDateDesc(String albumId);

    @Query("""
            SELECT d FROM AlbumDiscovery d
            WHERE d.album.id = :albumId AND d.creationDate <= :maximumCreationDate
              AND d.job.status IN (COMPLETE, MODERATE)
            ORDER BY d.creationDate DESC, d.id DESC
            """)
    Optional<AlbumDiscovery> findLatestForEvaluation(
            String albumId,
            LocalDateTime maximumCreationDate,
            Limit limit
    );

    @Query("""
            SELECT t.id FROM AlbumDiscovery d JOIN d.tasks t
            WHERE d.id = :discoveryId AND t.id > :afterTaskId AND t.creationDate <= :maximumCreationDate
            ORDER BY t.id
            """)
    List<String> findTaskIdsForEvaluation(
            String discoveryId,
            String afterTaskId,
            LocalDateTime maximumCreationDate,
            Pageable pageable
    );

    void deleteByAlbumId(String albumId);
}
