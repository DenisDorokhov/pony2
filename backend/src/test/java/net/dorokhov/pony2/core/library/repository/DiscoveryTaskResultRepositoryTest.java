package net.dorokhov.pony2.core.library.repository;

import jakarta.annotation.Nullable;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Test
    void shouldReadLatestCompletedArtistDataAcrossJobs() {
        Artist artist = saveArtist();
        Artist otherArtist = saveArtist();
        DiscoveryJob olderJob = saveDiscovery(artist, DiscoveryJob.Status.COMPLETE);
        DiscoveryJob currentJob = saveDiscovery(artist, DiscoveryJob.Status.STARTED);
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
        DiscoveryJob olderDiscovery = saveDiscovery(artist, DiscoveryJob.Status.COMPLETE);
        DiscoveryJob newerDiscovery = saveDiscovery(artist, DiscoveryJob.Status.FAILED);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask older = saveTask(artist, olderDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS,
                DiscoveryTask.Status.COMPLETE, date.minusHours(1), "[\"older\"]");
        jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", Timestamp.valueOf(date.plusHours(5)), older.getId());
        saveTask(artist, newerDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, "[\"latest\"]");
        saveTask(artist, newerDiscovery, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(1), "{}");
        for (DiscoveryTask.Status status : List.of(DiscoveryTask.Status.FAILED, DiscoveryTask.Status.INTERRUPTED, DiscoveryTask.Status.STARTED)) {
            saveTask(artist, newerDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, status, date.plusHours(2), "[\"incomplete\"]");
        }
        DiscoveryJob otherDiscovery = saveDiscovery(otherArtist, DiscoveryJob.Status.COMPLETE);
        saveTask(otherArtist, otherDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date.plusHours(3), "[\"other\"]");

        assertThat(taskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artist.getId(), Limit.of(1)))
                .contains("[\"latest\"]");
        assertThat(taskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(otherArtist.getId(), Limit.of(1)))
                .contains("[\"other\"]");
        assertThat(taskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId("missing", Limit.of(1))).isEmpty();
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
        DiscoveryJob olderJob = saveDiscovery(artist, DiscoveryJob.Status.COMPLETE);
        DiscoveryJob newerJob = saveDiscovery(artist, DiscoveryJob.Status.FAILED);
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
        DiscoveryJob discovery = saveDiscovery(artist, DiscoveryJob.Status.COMPLETE);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask first = saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, "[\"first\"]");
        DiscoveryTask second = saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, "[\"second\"]");
        DiscoveryTask expected = first.getId().compareTo(second.getId()) > 0 ? first : second;

        assertThat(taskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artist.getId(), Limit.of(1)))
                .contains(expected.getResult());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "[]", "[null]"})
    void shouldNotFallBackWhenLatestCompletedResultIsEmpty(@Nullable String result) {
        Artist artist = saveArtist();
        DiscoveryJob discovery = saveDiscovery(artist, DiscoveryJob.Status.COMPLETE);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date.minusHours(1), "[\"older\"]");
        saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, result);

        assertThat(taskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artist.getId(), Limit.of(1)))
                .isEqualTo(Optional.ofNullable(result));
    }

    @Test
    void shouldReturnEmptyWhenArtistHasNoCompletedTopTracks() {
        Artist artist = saveArtist();
        DiscoveryJob discovery = saveDiscovery(artist, DiscoveryJob.Status.STARTED);
        saveTask(artist, discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.STARTED,
                LocalDateTime.of(2026, 10, 16, 12, 0), null);

        assertThat(taskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artist.getId(), Limit.of(1))).isEmpty();
    }

    private Artist saveArtist() {
        return artistRepository.save(new Artist().setName("Artist"));
    }

    private DiscoveryJob saveDiscovery(Artist artist, DiscoveryJob.Status status) {
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
