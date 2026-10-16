package net.dorokhov.pony2.core.library.repository;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
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

class ArtistDiscoveryRepositoryTest extends IntegrationTest {

    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private ArtistDiscoveryRepository discoveryRepository;
    @Autowired
    private DiscoveryJobRepository jobRepository;
    @Autowired
    private DiscoveryTaskRepository taskRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReadLatestCompletedTopTracksForArtistAcrossJobs() {
        Artist artist = saveArtist();
        Artist otherArtist = saveArtist();
        ArtistDiscovery olderDiscovery = saveDiscovery(artist, DiscoveryJob.Status.COMPLETE);
        ArtistDiscovery newerDiscovery = saveDiscovery(artist, DiscoveryJob.Status.FAILED);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask older = saveTask(olderDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS,
                DiscoveryTask.Status.COMPLETE, date.minusHours(1), "[\"older\"]");
        jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", Timestamp.valueOf(date.plusHours(5)), older.getId());
        saveTask(newerDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, "[\"latest\"]");
        saveTask(newerDiscovery, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(1), "{}");
        for (DiscoveryTask.Status status : List.of(DiscoveryTask.Status.FAILED, DiscoveryTask.Status.INTERRUPTED, DiscoveryTask.Status.STARTED)) {
            saveTask(newerDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, status, date.plusHours(2), "[\"incomplete\"]");
        }
        ArtistDiscovery otherDiscovery = saveDiscovery(otherArtist, DiscoveryJob.Status.COMPLETE);
        saveTask(otherDiscovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date.plusHours(3), "[\"other\"]");

        assertThat(discoveryRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artist.getId(), Limit.of(1)))
                .contains("[\"latest\"]");
        assertThat(discoveryRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(otherArtist.getId(), Limit.of(1)))
                .contains("[\"other\"]");
        assertThat(discoveryRepository.findLatestCompletedSpotifyTopTracksResultByArtistId("missing", Limit.of(1))).isEmpty();
    }

    @Test
    void shouldBreakCreationDateTiesByTaskId() {
        Artist artist = saveArtist();
        ArtistDiscovery discovery = saveDiscovery(artist, DiscoveryJob.Status.COMPLETE);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        DiscoveryTask first = saveTask(discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, "[\"first\"]");
        DiscoveryTask second = saveTask(discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, "[\"second\"]");
        DiscoveryTask expected = first.getId().compareTo(second.getId()) > 0 ? first : second;

        assertThat(discoveryRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artist.getId(), Limit.of(1)))
                .contains(expected.getResult());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "[]", "[null]"})
    void shouldNotFallBackWhenLatestCompletedResultIsEmpty(@Nullable String result) {
        Artist artist = saveArtist();
        ArtistDiscovery discovery = saveDiscovery(artist, DiscoveryJob.Status.COMPLETE);
        LocalDateTime date = LocalDateTime.of(2026, 10, 16, 12, 0);
        saveTask(discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date.minusHours(1), "[\"older\"]");
        saveTask(discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE, date, result);

        assertThat(discoveryRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artist.getId(), Limit.of(1)))
                .isEqualTo(Optional.ofNullable(result));
    }

    @Test
    void shouldReturnEmptyWhenArtistHasNoCompletedTopTracks() {
        Artist artist = saveArtist();
        ArtistDiscovery discovery = saveDiscovery(artist, DiscoveryJob.Status.STARTED);
        saveTask(discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.STARTED,
                LocalDateTime.of(2026, 10, 16, 12, 0), null);

        assertThat(discoveryRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artist.getId(), Limit.of(1))).isEmpty();
    }

    private Artist saveArtist() {
        return artistRepository.save(new Artist().setName("Artist"));
    }

    private ArtistDiscovery saveDiscovery(Artist artist, DiscoveryJob.Status status) {
        DiscoveryJob job = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(status));
        return discoveryRepository.save(new ArtistDiscovery().setArtist(artist).setJob(job));
    }

    private DiscoveryTask saveTask(ArtistDiscovery discovery, DiscoveryTaskType type, DiscoveryTask.Status status,
                                   LocalDateTime creationDate, @Nullable String result) {
        return getTransactionTemplate().execute(transactionStatus -> {
            DiscoveryTask task = taskRepository.saveAndFlush(new DiscoveryTask().setJob(discovery.getJob()).setType(type)
                    .setStatus(status).setParameter("{}").setResult(result));
            ArtistDiscovery attached = discoveryRepository.findById(discovery.getId()).orElseThrow();
            attached.getTasks().add(task);
            discoveryRepository.save(attached);
            jdbcTemplate.update("UPDATE discovery_task SET creation_date = ? WHERE id = ?", Timestamp.valueOf(creationDate), task.getId());
            return task;
        });
    }
}
