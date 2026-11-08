package net.dorokhov.pony2.core.library.repository;

import jakarta.persistence.EntityManagerFactory;
import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.core.library.service.scan.BatchLibraryCleaner;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryTaskSummaryRepositoryTest extends IntegrationTest {

    @Autowired private DiscoveryTaskRepository taskRepository;
    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private SongRepository songRepository;
    @Autowired private DiscoveryJobService discoveryJobService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private BatchLibraryCleaner batchLibraryCleaner;

    @Test
    void shouldExcludePayloadColumnsFromEveryTaskListQueryAndComputeNoResult() {
        Artist artist = artistRepository.save(new Artist().setName("Artist"));
        Album album = albumRepository.save(new Album().setArtist(artist).setName("Album"));
        DiscoveryJob job = saveJob();
        Map<String, Boolean> expectedNoResults = new LinkedHashMap<>();
        for (DiscoveryTask.Status status : DiscoveryTask.Status.values()) {
            for (String result : Arrays.asList(null, "", "null", "{}")) {
                DiscoveryTask task = taskRepository.save(task(job).setAlbum(album).setStatus(status).setResult(result));
                expectedNoResults.put(task.getId(), status == DiscoveryTask.Status.COMPLETE && result == null);
            }
        }
        jdbcTemplate.update("UPDATE discovery_task SET raw_requests = ?, raw_responses = ?",
                "not valid request JSON", "not valid response JSON");
        List<String> statements = new ArrayList<>();
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        StatementInspector statementInspector = sql -> {
            statements.add(sql);
            return sql;
        };
        try (Session session = sessionFactory.withOptions().statementInspector(statementInspector).openSession()) {
            DiscoveryTaskRepository repository = new JpaRepositoryFactory(session).getRepository(DiscoveryTaskRepository.class);
            PageRequest pageRequest = PageRequest.of(0, 1);
            DiscoveryTaskType type = DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA;
            List<Supplier<Page<DiscoveryTaskSummary>>> queries = new ArrayList<>(List.of(
                    () -> repository.findAllTasks(pageRequest),
                    () -> repository.findByType(type, pageRequest),
                    () -> repository.findFailed(pageRequest),
                    () -> repository.findFailedByType(type, pageRequest),
                    () -> repository.findWithoutResult(pageRequest),
                    () -> repository.findWithoutResultByType(type, pageRequest),
                    () -> repository.findByJobId(job.getId(), pageRequest),
                    () -> repository.findByJobIdAndType(job.getId(), type, pageRequest),
                    () -> repository.findFailedByJobId(job.getId(), pageRequest),
                    () -> repository.findFailedByJobIdAndType(job.getId(), type, pageRequest),
                    () -> repository.findWithoutResultByJobId(job.getId(), pageRequest),
                    () -> repository.findWithoutResultByJobIdAndType(job.getId(), type, pageRequest)));
            for (DiscoveryTaskType filterType : Arrays.asList(null, type)) {
                for (DiscoveryTaskFilter.Result result : Arrays.asList(null, DiscoveryTaskFilter.Result.FAILED,
                        DiscoveryTaskFilter.Result.NO_RESULT)) {
                    boolean failed = result == DiscoveryTaskFilter.Result.FAILED;
                    boolean withoutResult = result == DiscoveryTaskFilter.Result.NO_RESULT;
                    queries.add(() -> repository.findByArtistId(artist.getId(), filterType, failed, withoutResult, pageRequest));
                    queries.add(() -> repository.findByAlbumId(album.getId(), filterType, failed, withoutResult, pageRequest));
                }
            }
            for (Supplier<Page<DiscoveryTaskSummary>> query : queries) {
                Page<DiscoveryTaskSummary> page = query.get();

                assertThat(page.getTotalElements()).isPositive();
                assertThat(page.getContent()).hasSize(1).allSatisfy(summary ->
                        assertThat(summary.noResult()).isEqualTo(expectedNoResults.get(summary.id())));
            }
            assertThat(repository.findAllTasks(PageRequest.of(0, 30)).getContent())
                    .hasSize(expectedNoResults.size())
                    .allSatisfy(summary -> assertThat(summary.noResult()).isEqualTo(expectedNoResults.get(summary.id())));
        }

        assertThat(statements).isNotEmpty().anyMatch(sql -> sql.contains("count("));
        assertThat(statements).allSatisfy(sql -> {
            assertThat(sql).doesNotContain("raw_requests", "raw_responses");
            String selectClause = sql.substring(0, sql.toLowerCase(Locale.ROOT).indexOf(" from "));
            String selectWithoutResultNullChecks = selectClause.replaceAll("(?i)\\b\\w+\\.result\\s+is\\s+null\\b", "");
            assertThat(selectWithoutResultNullChecks).doesNotContain(".result");
        });
    }

    private DiscoveryJob saveJob() {
        return jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.COMPLETE));
    }

    private DiscoveryTask task(DiscoveryJob job) {
        return new DiscoveryTask().setJob(job).setType(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA)
                .setStatus(DiscoveryTask.Status.COMPLETE);
    }

    @Test
    void shouldPreserveEveryTargetKindAndHistoryAfterDeletingTargets() {
        Artist artist = artistRepository.save(new Artist().setName("Artist"));
        Album album = albumRepository.save(new Album().setArtist(artist).setName("Album"));
        Genre genre = genreRepository.save(new Genre().setName("Genre"));
        Song song = songRepository.save(song().setAlbum(album).setGenre(genre));
        DiscoveryJob job = saveJob();
        DiscoveryTask artistTask = taskRepository.save(task(job).setArtist(artist));
        DiscoveryTask albumTask = taskRepository.save(task(job).setAlbum(album));
        DiscoveryTask genreTask = taskRepository.save(task(job).setGenre(genre));
        DiscoveryTask songTask = taskRepository.save(task(job).setSong(song));
        DiscoveryTask targetlessTask = taskRepository.save(task(job));
        DiscoveryTaskFilter filter = new DiscoveryTaskFilter(null, null);
        PageRequest pageRequest = PageRequest.of(0, 30);

        List<DiscoveryTaskSummary> summaries = discoveryJobService.getAllTasks(filter, pageRequest).getContent();

        assertThat(summaries).hasSize(5);
        assertThat(summaries).filteredOn(summary -> summary.id().equals(artistTask.getId()))
                .singleElement().satisfies(summary -> assertThat(summary.artist().getId()).isEqualTo(artist.getId()));
        assertThat(summaries).filteredOn(summary -> summary.id().equals(albumTask.getId()))
                .singleElement().satisfies(summary -> assertThat(summary.album().getId()).isEqualTo(album.getId()));
        assertThat(summaries).filteredOn(summary -> summary.id().equals(genreTask.getId()))
                .singleElement().satisfies(summary -> assertThat(summary.genre().getId()).isEqualTo(genre.getId()));
        assertThat(summaries).filteredOn(summary -> summary.id().equals(songTask.getId()))
                .singleElement().satisfies(summary -> assertThat(summary.song().getId()).isEqualTo(song.getId()));
        assertThat(summaries).filteredOn(summary -> summary.id().equals(targetlessTask.getId()))
                .singleElement().satisfies(summary -> {
                    assertThat(summary.artist()).isNull();
                    assertThat(summary.album()).isNull();
                    assertThat(summary.genre()).isNull();
                    assertThat(summary.song()).isNull();
                });

        batchLibraryCleaner.cleanSongs(List.of(), (itemsComplete, itemsTotal) -> {});

        for (Page<DiscoveryTaskSummary> page : List.of(discoveryJobService.getAllTasks(filter, pageRequest),
                discoveryJobService.getJobTasks(job.getId(), filter, pageRequest))) {
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getContent()).extracting(DiscoveryTaskSummary::id).containsExactlyInAnyOrder(
                    artistTask.getId(), albumTask.getId(), genreTask.getId(), songTask.getId(), targetlessTask.getId());
            assertThat(page.getContent()).allSatisfy(summary -> {
                assertThat(summary.artist()).isNull();
                assertThat(summary.album()).isNull();
                assertThat(summary.genre()).isNull();
                assertThat(summary.song()).isNull();
                assertThat(summary.noResult()).isTrue();
            });
        }
    }

    @Test
    void shouldKeepFullPayloadsWhenLoadingTaskDetails() {
        List<Object> requests = List.of(Map.of("prompt", "Artist"));
        List<Object> responses = List.of(Map.of("response", "Biography"));
        DiscoveryTask task = taskRepository.save(task(saveJob()).setResult("{\"biography\":\"Biography\"}")
                .setRawRequests(requests).setRawResponses(responses));

        DiscoveryTask loaded = discoveryJobService.getTaskById(task.getId()).orElseThrow();

        assertThat(loaded.getResult()).isEqualTo(task.getResult());
        assertThat(loaded.getRawRequests()).isEqualTo(requests);
        assertThat(loaded.getRawResponses()).isEqualTo(responses);
    }
}
