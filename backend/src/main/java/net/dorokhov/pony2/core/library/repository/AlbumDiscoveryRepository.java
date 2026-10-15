package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.AlbumDiscovery;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
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
            SELECT COUNT(t) FROM AlbumDiscovery d JOIN d.tasks t
            WHERE d.id = :discoveryId AND t.status = COMPLETE
            """)
    long countCompletedTasksByDiscoveryId(String discoveryId);

    @Query("""
            SELECT t.result FROM AlbumDiscovery d JOIN d.tasks t
            WHERE d.album.id = :albumId AND d.job.id = :jobId
              AND t.type = :taskType AND t.status = COMPLETE
            """)
    Optional<String> findCompletedTaskResultByAlbumIdAndJobIdAndType(String albumId, String jobId, DiscoveryTaskType taskType);

    @Query("""
            SELECT t.result FROM AlbumDiscovery d JOIN d.tasks t
            WHERE d.album.id = :albumId AND t.type = MUSICBRAINZ_ALBUM_DATA AND t.status = COMPLETE
            ORDER BY t.creationDate DESC, t.id DESC
            """)
    Optional<String> findLatestCompletedMusicBrainzResultByAlbumId(String albumId, Limit limit);

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
