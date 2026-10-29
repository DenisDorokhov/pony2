package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiscoveryTaskRepository extends JpaRepository<DiscoveryTask, String> {

    List<DiscoveryTask> findByStatus(DiscoveryTask.Status status);

    long countByJobId(String jobId);

    @Query("""
            SELECT t FROM DiscoveryTask t
            ORDER BY COALESCE(t.updateDate, t.creationDate) DESC, t.id DESC
            """)
    Page<DiscoveryTask> findAllTasks(Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.type = :type
            ORDER BY t.type, COALESCE(t.updateDate, t.creationDate) DESC, t.id DESC
            """)
    Page<DiscoveryTask> findByType(DiscoveryTaskType type, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.status = FAILED
            ORDER BY t.status DESC, t.updateDate DESC NULLS LAST, t.id DESC
            """)
    Page<DiscoveryTask> findFailed(Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.status = FAILED AND t.type = :type
            ORDER BY t.status, t.type, t.updateDate DESC NULLS LAST, t.id DESC
            """)
    Page<DiscoveryTask> findFailedByType(DiscoveryTaskType type, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.status = COMPLETE AND t.result IS NULL
            ORDER BY t.status, t.result, t.updateDate DESC NULLS LAST, t.id DESC
            """)
    Page<DiscoveryTask> findWithoutResult(Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.status = COMPLETE AND t.type = :type AND t.result IS NULL
            ORDER BY t.status, t.type, t.result, t.updateDate DESC NULLS LAST, t.id DESC
            """)
    Page<DiscoveryTask> findWithoutResultByType(DiscoveryTaskType type, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.job.id = :jobId
            ORDER BY t.job.id, COALESCE(t.updateDate, t.creationDate) DESC, t.id DESC
            """)
    Page<DiscoveryTask> findByJobId(String jobId, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.job.id = :jobId AND t.type = :type
            ORDER BY t.job.id, t.type, COALESCE(t.updateDate, t.creationDate) DESC, t.id DESC
            """)
    Page<DiscoveryTask> findByJobIdAndType(String jobId, DiscoveryTaskType type, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.job.id = :jobId AND t.status = FAILED
            ORDER BY t.job.id, t.status, t.updateDate DESC NULLS LAST, t.id DESC
            """)
    Page<DiscoveryTask> findFailedByJobId(String jobId, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.job.id = :jobId AND t.status = FAILED AND t.type = :type
            ORDER BY t.job.id, t.status, t.type, t.updateDate DESC NULLS LAST, t.id DESC
            """)
    Page<DiscoveryTask> findFailedByJobIdAndType(String jobId, DiscoveryTaskType type, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.job.id = :jobId AND t.status = COMPLETE AND t.result IS NULL
            ORDER BY t.job.id, t.status, t.result, t.updateDate DESC NULLS LAST, t.id DESC
            """)
    Page<DiscoveryTask> findWithoutResultByJobId(String jobId, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.job.id = :jobId AND t.status = COMPLETE AND t.type = :type AND t.result IS NULL
            ORDER BY t.job.id, t.status, t.type, t.result, t.updateDate DESC NULLS LAST, t.id DESC
            """)
    Page<DiscoveryTask> findWithoutResultByJobIdAndType(String jobId, DiscoveryTaskType type, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t LEFT JOIN t.album a
            WHERE (t.artist.id = :artistId OR a.artist.id = :artistId)
              AND (:type IS NULL OR t.type = :type)
              AND (:failed = false OR t.status = FAILED)
              AND (:withoutResult = false OR (t.status = COMPLETE AND t.result IS NULL))
            ORDER BY COALESCE(t.updateDate, t.creationDate) DESC, t.id DESC
            """)
    Page<DiscoveryTask> findByArtistId(String artistId, DiscoveryTaskType type, boolean failed, boolean withoutResult, Pageable pageable);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.album.id = :albumId
              AND (:type IS NULL OR t.type = :type)
              AND (:failed = false OR t.status = FAILED)
              AND (:withoutResult = false OR (t.status = COMPLETE AND t.result IS NULL))
            ORDER BY COALESCE(t.updateDate, t.creationDate) DESC, t.id DESC
            """)
    Page<DiscoveryTask> findByAlbumId(String albumId, DiscoveryTaskType type, boolean failed, boolean withoutResult, Pageable pageable);

    long countByJobIdAndStatus(String jobId, DiscoveryTask.Status status);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.genre.id = :genreId AND t.type = :type AND t.status = COMPLETE
            ORDER BY t.genre.id, t.status, t.type, t.creationDate DESC, t.id DESC
            """)
    Optional<DiscoveryTask> findLatestCompletedByGenreIdAndType(String genreId, DiscoveryTaskType type, Limit limit);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.artist.id = :artistId AND t.type = :type AND t.status = COMPLETE
            ORDER BY t.artist.id, t.status, t.type, t.creationDate DESC, t.id DESC
            """)
    Optional<DiscoveryTask> findLatestCompletedByArtistIdAndType(String artistId, DiscoveryTaskType type, Limit limit);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.album.id = :albumId AND t.type = :type AND t.status = COMPLETE
            ORDER BY t.album.id, t.status, t.type, t.creationDate DESC, t.id DESC
            """)
    Optional<DiscoveryTask> findLatestCompletedByAlbumIdAndType(String albumId, DiscoveryTaskType type, Limit limit);

    @Query("""
            SELECT t.creationDate FROM DiscoveryTask t
            WHERE t.artist.id = :artistId AND t.type = :type AND t.status = COMPLETE
            ORDER BY t.artist.id, t.status, t.type, t.creationDate DESC, t.id DESC
            """)
    Optional<LocalDateTime> findLatestCompletedCreationDateByArtistIdAndType(String artistId, DiscoveryTaskType type, Limit limit);

    @Query("""
            SELECT t.creationDate FROM DiscoveryTask t
            WHERE t.album.id = :albumId AND t.type = :type AND t.status = COMPLETE
            ORDER BY t.album.id, t.status, t.type, t.creationDate DESC, t.id DESC
            """)
    Optional<LocalDateTime> findLatestCompletedCreationDateByAlbumIdAndType(String albumId, DiscoveryTaskType type, Limit limit);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.song.id = :songId AND t.type = :type AND t.status = COMPLETE
            ORDER BY t.song.id, t.status, t.type, t.creationDate DESC, t.id DESC
            """)
    Optional<DiscoveryTask> findLatestCompletedBySongIdAndType(String songId, DiscoveryTaskType type, Limit limit);

    @Query("""
            SELECT t.result FROM DiscoveryTask t
            WHERE t.artist.id = :artistId AND t.type = SPOTIFY_TOP_TRACKS AND t.status = COMPLETE
            ORDER BY t.artist.id, t.status, t.type, t.creationDate DESC, t.id DESC
            """)
    Optional<String> findLatestCompletedSpotifyTopTracksResultByArtistId(String artistId, Limit limit);

    @Query("""
            SELECT t.result FROM DiscoveryTask t
            WHERE t.album.id = :albumId AND t.type = MUSICBRAINZ_ALBUM_DATA AND t.status = COMPLETE
            ORDER BY t.album.id, t.status, t.type, t.creationDate DESC, t.id DESC
            """)
    Optional<String> findLatestCompletedMusicBrainzResultByAlbumId(String albumId, Limit limit);

    @Modifying
    @Query("DELETE FROM DiscoveryTask t WHERE t.job.id = :jobId")
    void deleteByJobId(String jobId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE DiscoveryTask t SET t.genre = NULL WHERE t.genre.id = :genreId")
    void clearGenreByGenreId(String genreId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE DiscoveryTask t SET t.artist = NULL WHERE t.artist.id = :artistId")
    void clearArtistByArtistId(String artistId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE DiscoveryTask t SET t.album = NULL WHERE t.album.id = :albumId")
    void clearAlbumByAlbumId(String albumId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE DiscoveryTask t SET t.song = NULL WHERE t.song.id = :songId")
    void clearSongBySongId(String songId);

    @Query("""
            SELECT DISTINCT t.genre.id FROM DiscoveryTask t
            WHERE t.genre.id > :afterId AND t.status = COMPLETE
            ORDER BY t.genre.id
            """)
    List<String> findGenreIdsForEvaluation(String afterId, Pageable pageable);

    @Query("""
            SELECT DISTINCT t.artist.id FROM DiscoveryTask t
            WHERE t.artist.id > :afterId AND t.status = COMPLETE
            ORDER BY t.artist.id
            """)
    List<String> findArtistIdsForEvaluation(String afterId, Pageable pageable);

    @Query("""
            SELECT DISTINCT t.album.id FROM DiscoveryTask t
            WHERE t.album.id > :afterId AND t.status = COMPLETE
            ORDER BY t.album.id
            """)
    List<String> findAlbumIdsForEvaluation(String afterId, Pageable pageable);

    @Query("""
            SELECT DISTINCT t.song.id FROM DiscoveryTask t
            WHERE t.song.id > :afterId AND t.status = COMPLETE
            ORDER BY t.song.id
            """)
    List<String> findSongIdsForEvaluation(String afterId, Pageable pageable);
}
