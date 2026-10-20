package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.llm.domain.DiscoveryCacheRegion;
import net.dorokhov.pony2.api.llm.service.DiscoveryCacheService;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.llm.repository.DiscoveryCacheRepository;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.ErrorDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscoveryDataCleanupTest extends InstallingIntegrationTest {

    private static final String PATH = "/api/admin/discovery/data";

    @Autowired private ApiTemplate apiTemplate;
    @Autowired private DiscoveryJobService discoveryJobService;
    @Autowired private LibraryJobSynchronizer jobSynchronizer;
    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private DiscoveryTaskRepository taskRepository;
    @Autowired private ArtistDiscoveryIndexRepository indexRepository;
    @Autowired private DiscoveryCacheRepository cacheRepository;
    @Autowired private DiscoveryCacheService cacheService;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private SongRepository songRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCleanAllDiscoveryDataAndKeepLibrary() throws Exception {
        populate();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(PATH, HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(taskRepository.count()).isZero();
        assertThat(jobRepository.count()).isZero();
        assertThat(indexRepository.count()).isZero();
        assertThat(cacheRepository.count()).isZero();
        assertThat(artistRepository.count()).isOne();
        assertThat(albumRepository.count()).isOne();
        assertThat(genreRepository.count()).isOne();
        assertThat(songRepository.count()).isOne();
        discoveryJobService.cleanDiscoveryData();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRejectCleanupWhileDiscoveryOrScanOwnsLibraryWithoutCancellation(boolean discovery) throws Exception {
        populate();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        try (LibraryJobSynchronizer.LibraryJobRegistration ignored = discovery
                ? jobSynchronizer.registerDiscoveryJob() : jobSynchronizer.registerScanJob(Duration.ZERO)) {
            boolean previouslyCancelled = jobSynchronizer.isDiscoveryCancelled();

            ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(PATH, HttpMethod.DELETE,
                    apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorDto.Code.CONCURRENT_LIBRARY_JOB);
            assertThat(jobSynchronizer.isDiscoveryCancelled()).isEqualTo(previouslyCancelled);
            assertThat(jobRepository.count()).isEqualTo(2);
            assertThat(taskRepository.count()).isEqualTo(4);
            assertThat(indexRepository.count()).isOne();
            assertThat(cacheRepository.count()).isOne();
        }
    }

    @Test
    void shouldCommitEachJobSeparatelyAndRollbackTasksOfFailingJob() throws Exception {
        populate();
        List<String> ids = jobRepository.findAll().stream().map(DiscoveryJob::getId).sorted().toList();
        jdbcTemplate.execute("CREATE TABLE cleanup_blocker (job_id CHAR(36) REFERENCES discovery_job(id))");
        jdbcTemplate.update("INSERT INTO cleanup_blocker (job_id) VALUES (?)", ids.getLast());

        assertThatThrownBy(discoveryJobService::cleanDiscoveryData).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jobRepository.findById(ids.getFirst())).isEmpty();
        assertThat(taskRepository.countByJobId(ids.getFirst())).isZero();
        assertThat(jobRepository.findById(ids.getLast())).isPresent();
        assertThat(taskRepository.countByJobId(ids.getLast())).isEqualTo(2);
        assertThat(indexRepository.count()).isOne();
        assertThat(cacheRepository.count()).isOne();
        try (LibraryJobSynchronizer.LibraryJobRegistration ignored = jobSynchronizer.registerMaintenanceJob()) {
            assertThat(jobSynchronizer.hasRunningTasks()).isFalse();
        }
        jdbcTemplate.update("DELETE FROM cleanup_blocker");
        discoveryJobService.cleanDiscoveryData();
        assertThat(jobRepository.count()).isZero();
        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void shouldRejectAnonymousCleanup() {
        assertThat(apiTemplate.getRestTemplate().exchange(PATH, HttpMethod.DELETE, HttpEntity.EMPTY, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void populate() {
        Artist artist = artistRepository.save(new Artist().setName("Artist"));
        Album album = albumRepository.save(new Album().setArtist(artist).setName("Album"));
        Genre genre = genreRepository.save(new Genre().setName("Genre"));
        Song song = songRepository.save(song().setAlbum(album).setGenre(genre));
        DiscoveryJob first = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.COMPLETE));
        DiscoveryJob second = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.FAILED));
        List<DiscoveryJob> jobs = List.of(first, second).stream().sorted(Comparator.comparing(DiscoveryJob::getId)).toList();
        taskRepository.save(task(jobs.getFirst()).setArtist(artist));
        taskRepository.save(task(jobs.getFirst()).setAlbum(album));
        taskRepository.save(task(jobs.getLast()).setGenre(genre));
        taskRepository.save(task(jobs.getLast()).setSong(song));
        indexRepository.save(new ArtistDiscoveryIndex().setArtist(artist).setSpotifyArtistId("spotify"));
        cacheService.put(DiscoveryCacheRegion.SPOTIFY, "key", 1, "value");
    }

    private DiscoveryTask task(DiscoveryJob job) {
        return new DiscoveryTask().setJob(job).setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .setStatus(DiscoveryTask.Status.COMPLETE).setResult("{}");
    }
}
