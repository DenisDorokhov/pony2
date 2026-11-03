package net.dorokhov.pony2.core.library.repository;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManagerFactory;
import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryTaskResultRepositoryTest extends IntegrationTest {

    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private DiscoveryJobRepository jobRepository;
    @Autowired
    private DiscoveryTaskRepository taskRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldReadLatestCompletedArtistDataAcrossJobs() {
        Artist artist = saveArtist();
        Artist otherArtist = saveArtist();
        DiscoveryJob olderJob = saveDiscovery(DiscoveryJob.Status.COMPLETE);
        DiscoveryJob currentJob = saveDiscovery(DiscoveryJob.Status.STARTED);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask older = saveTask(artist, olderJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
                DiscoveryTask.Status.COMPLETE, date.minusHours(1), "older");
        jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", Timestamp.valueOf(date.plusHours(5)), older.getId());
        DiscoveryTask latest = saveTask(artist, currentJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
                DiscoveryTask.Status.COMPLETE, date, "latest");
        for (DiscoveryTask.Status status : List.of(DiscoveryTask.Status.FAILED, DiscoveryTask.Status.INTERRUPTED, DiscoveryTask.Status.STARTED)) {
            saveTask(artist, currentJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, status, date.plusHours(1), "incomplete");
        }
        saveTask(artist, currentJob, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date.plusHours(2), "wrong type");
        saveTask(otherArtist, currentJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(3), "other artist");

        assertThat(taskRepository.findLatestCompletedByArtistIdAndType(artist.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .get().extracting(DiscoveryTask::getId).isEqualTo(latest.getId());

        taskRepository.deleteById(latest.getId());

        assertThat(taskRepository.findLatestCompletedByArtistIdAndType(artist.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .get().extracting(DiscoveryTask::getId).isEqualTo(older.getId());
        assertThat(taskRepository.findLatestCompletedByArtistIdAndType("missing", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .isEmpty();
    }

    @Test
    void shouldReadLatestCompletedTopTracksForArtistAcrossJobs() {
        Artist artist = saveArtist();
        Artist otherArtist = saveArtist();
        DiscoveryJob olderDiscovery = saveDiscovery(DiscoveryJob.Status.COMPLETE);
        DiscoveryJob newerDiscovery = saveDiscovery(DiscoveryJob.Status.FAILED);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask older = saveTask(artist, olderDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS,
                DiscoveryTask.Status.COMPLETE, date.minusHours(1), "[\"older\"]");
        jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", Timestamp.valueOf(date.plusHours(5)), older.getId());
        saveTask(artist, newerDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, "[\"latest\"]");
        saveTask(artist, newerDiscovery, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(1), "{}");
        for (DiscoveryTask.Status status : List.of(DiscoveryTask.Status.FAILED, DiscoveryTask.Status.INTERRUPTED, DiscoveryTask.Status.STARTED)) {
            saveTask(artist, newerDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, status, date.plusHours(2), "[\"incomplete\"]");
        }
        DiscoveryJob otherDiscovery = saveDiscovery(DiscoveryJob.Status.COMPLETE);
        saveTask(otherArtist, otherDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date.plusHours(3), "[\"other\"]");

        assertThat(taskRepository.findLatestCompletedResultByArtistIdAndType(artist.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1)))
                .contains("[\"latest\"]");
        assertThat(taskRepository.findLatestCompletedResultByArtistIdAndType(otherArtist.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1)))
                .contains("[\"other\"]");
        assertThat(taskRepository.findLatestCompletedResultByArtistIdAndType("missing", DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1))).isEmpty();
        assertThat(taskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                artist.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1))).contains(date);
        assertThat(taskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                artist.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1))).contains(date.plusHours(1));
        assertThat(taskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                otherArtist.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1))).contains(date.plusHours(3));
        assertThat(taskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                "missing", DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1))).isEmpty();
    }

    @Test
    void shouldReadLatestCompletedCreationDateForAlbumAcrossJobs() {
        Artist artist = saveArtist();
        Album album = albumRepository.save(new Album().setArtist(artist).setName("Album"));
        DiscoveryJob olderJob = saveDiscovery(DiscoveryJob.Status.COMPLETE);
        DiscoveryJob newerJob = saveDiscovery(DiscoveryJob.Status.FAILED);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask older = saveAlbumTask(album, olderJob, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA,
                DiscoveryTask.Status.COMPLETE, date.minusHours(1));
        jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", Timestamp.valueOf(date.plusHours(5)), older.getId());
        saveAlbumTask(album, newerJob, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.COMPLETE, date);
        saveAlbumTask(album, newerJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(1));
        for (DiscoveryTask.Status status : List.of(DiscoveryTask.Status.FAILED, DiscoveryTask.Status.INTERRUPTED, DiscoveryTask.Status.STARTED)) {
            saveAlbumTask(album, newerJob, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, status, date.plusHours(2));
        }

        assertThat(taskRepository.findLatestCompletedCreationDateByAlbumIdAndType(
                album.getId(), DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, Limit.of(1))).contains(date);
        assertThat(taskRepository.findLatestCompletedCreationDateByAlbumIdAndType(
                album.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1))).contains(date.plusHours(1));
        assertThat(taskRepository.findLatestCompletedCreationDateByAlbumIdAndType(
                "missing", DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, Limit.of(1))).isEmpty();
    }

    @Test
    void shouldBreakCreationDateTiesByTaskId() {
        Artist artist = saveArtist();
        DiscoveryJob discovery = saveDiscovery(DiscoveryJob.Status.COMPLETE);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask first = saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, "[\"first\"]");
        DiscoveryTask second = saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, "[\"second\"]");
        DiscoveryTask expected = first.getId().compareTo(second.getId()) > 0 ? first : second;

        assertThat(taskRepository.findLatestCompletedResultByArtistIdAndType(artist.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1)))
                .contains(expected.getResult());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "[]", "[null]"})
    void shouldNotFallBackWhenLatestCompletedResultIsEmpty(@Nullable String result) {
        Artist artist = saveArtist();
        DiscoveryJob discovery = saveDiscovery(DiscoveryJob.Status.COMPLETE);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date.minusHours(1), "[\"older\"]");
        saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, result);

        assertThat(taskRepository.findLatestCompletedResultByArtistIdAndType(artist.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1)))
                .isEqualTo(Optional.ofNullable(result));
    }

    @Test
    void shouldReturnEmptyWhenArtistHasNoCompletedTopTracks() {
        Artist artist = saveArtist();
        DiscoveryJob discovery = saveDiscovery(DiscoveryJob.Status.STARTED);
        saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.STARTED,
                LocalDateTime.of(2026, 10, 16, 12, 0), null);

        assertThat(taskRepository.findLatestCompletedResultByArtistIdAndType(artist.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1))).isEmpty();
    }

    @Test
    void shouldReadLatestCompletedResultsInOneBatchIncludingNullResults() {
        Artist first = saveArtist();
        Artist second = saveArtist();
        Artist unfinished = saveArtist();
        Artist outside = saveArtist();
        DiscoveryJob job = saveDiscovery(DiscoveryJob.Status.COMPLETE);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask older = saveTask(first, job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
                DiscoveryTask.Status.COMPLETE, date.minusHours(1), "older");
        jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", Timestamp.valueOf(date.plusHours(5)), older.getId());
        saveTask(first, job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date, null);
        saveTask(second, job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date, "latest");
        saveTask(second, job, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date.plusHours(1), "wrong type");
        for (DiscoveryTask.Status status : List.of(DiscoveryTask.Status.FAILED, DiscoveryTask.Status.INTERRUPTED, DiscoveryTask.Status.STARTED)) {
            saveTask(second, job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, status, date.plusHours(2), "incomplete");
        }
        saveTask(unfinished, job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.STARTED, date, "unfinished");
        saveTask(outside, job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date, "outside");
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            List<DiscoveryTaskRepository.ArtistResult> results = taskRepository.findLatestCompletedResultsByArtistIdsAndType(
                    List.of(first.getId(), second.getId(), unfinished.getId(), "missing"), DiscoveryTaskType.SPOTIFY_ARTIST_DATA);

            assertThat(results).extracting(DiscoveryTaskRepository.ArtistResult::getArtistId)
                    .containsExactlyInAnyOrder(first.getId(), second.getId(), unfinished.getId());
            assertThat(results).filteredOn(result -> result.getArtistId().equals(first.getId()))
                    .singleElement().extracting(DiscoveryTaskRepository.ArtistResult::getResult).isNull();
            assertThat(results).filteredOn(result -> result.getArtistId().equals(second.getId()))
                    .singleElement().extracting(DiscoveryTaskRepository.ArtistResult::getResult).isEqualTo("latest");
            assertThat(results).filteredOn(result -> result.getArtistId().equals(unfinished.getId()))
                    .singleElement().extracting(DiscoveryTaskRepository.ArtistResult::getResult).isNull();
            assertThat(statistics.getPrepareStatementCount()).isOne();
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }

    @Test
    void shouldBreakBatchResultCreationDateTiesByTaskIdAndAcceptAnyTaskType() {
        Artist artist = saveArtist();
        DiscoveryJob job = saveDiscovery(DiscoveryJob.Status.COMPLETE);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask first = saveTask(artist, job, DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES,
                DiscoveryTask.Status.COMPLETE, date, "first");
        DiscoveryTask second = saveTask(artist, job, DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES,
                DiscoveryTask.Status.COMPLETE, date, "second");
        DiscoveryTask expected = first.getId().compareTo(second.getId()) > 0 ? first : second;
        saveTask(artist, job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(1), "wrong type");

        assertThat(taskRepository.findLatestCompletedResultsByArtistIdsAndType(List.of(artist.getId()), DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES))
                .singleElement().satisfies(result -> {
                    assertThat(result.getArtistId()).isEqualTo(artist.getId());
                    assertThat(result.getResult()).isEqualTo(expected.getResult());
                });
    }

    @Test
    void shouldUseExistingArtistIndexForLimitedBatchLatestResultLookup() {
        String plan = jdbcTemplate.queryForObject("""
                EXPLAIN SELECT artist.id,
                    (SELECT t.result FROM discovery_task t
                     WHERE t.artist_id = artist.id AND t.type = ? AND t.status = 'COMPLETE'
                     ORDER BY t.artist_id, t.status, t.type, t.creation_date DESC, t.id DESC
                     FETCH FIRST 1 ROW ONLY) AS result
                FROM artist WHERE artist.id IN (?, ?)
                """, String.class, DiscoveryTaskType.SPOTIFY_ARTIST_DATA.name(), "first", "second");

        assertThat(plan.toLowerCase()).contains("index_discovery_task_artist_status_type_creation_date", "index sorted")
                .doesNotContain("tablescan");
    }

    private Artist saveArtist() {
        return artistRepository.save(new Artist().setName("Artist"));
    }

    @ParameterizedTest
    @CsvSource({
            "artist_id,index_discovery_task_artist_status_type_creation_date",
            "album_id,index_discovery_task_album_status_type_creation_date"
    })
    void shouldUseExistingIndexForParameterizedLatestResult(String column, String index) {
        String plan = jdbcTemplate.queryForObject("EXPLAIN SELECT result FROM discovery_task WHERE " + column +
                        " = ? AND type = ? AND status = 'COMPLETE' ORDER BY " + column +
                        ", status, type, creation_date DESC, id DESC FETCH FIRST 1 ROWS ONLY",
                String.class, "target", DiscoveryTaskType.SPOTIFY_ARTIST_DATA.name());

        assertThat(plan.toLowerCase()).contains(index, "index sorted");
    }

    private DiscoveryJob saveDiscovery(DiscoveryJob.Status status) {
        return jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(status));
    }

    private DiscoveryTask saveTask(Artist artist, DiscoveryJob job, DiscoveryTaskType type, DiscoveryTask.Status status,
                                   LocalDateTime creationDate, @Nullable String result) {
        return getTransactionTemplate().execute(transactionStatus -> {
            DiscoveryTask task = taskRepository.saveAndFlush(new DiscoveryTask().setJob(job).setArtist(artist).setType(type)
                    .setStatus(status).setResult(result));
            jdbcTemplate.update("UPDATE discovery_task SET creation_date = ? WHERE id = ?", Timestamp.valueOf(creationDate), task.getId());
            return task;
        });
    }

    private DiscoveryTask saveAlbumTask(Album album, DiscoveryJob job, DiscoveryTaskType type, DiscoveryTask.Status status,
                                        LocalDateTime creationDate) {
        return getTransactionTemplate().execute(transactionStatus -> {
            DiscoveryTask task = taskRepository.saveAndFlush(new DiscoveryTask().setJob(job).setAlbum(album).setType(type)
                    .setStatus(status));
            jdbcTemplate.update("UPDATE discovery_task SET creation_date = ? WHERE id = ?", Timestamp.valueOf(creationDate), task.getId());
            return task;
        });
    }
}
