package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskFilter;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryTaskRepositoryTest extends IntegrationTest {

    @Autowired
    private DiscoveryJobRepository jobRepository;
    @Autowired
    private DiscoveryTaskRepository taskRepository;
    @Autowired
    private DiscoveryJobService discoveryJobService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldPageAllTasksAcrossJobsWithStableOrderByLastActivity(boolean filterType) {
        DiscoveryJob job = saveJob();
        DiscoveryJob otherJob = saveJob();
        LocalDateTime date = LocalDateTime.of(2026, 10, 15, 12, 0);
        DiscoveryTask older = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.minusHours(1));
        DiscoveryTask firstArtist = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.FAILED, date);
        DiscoveryTask secondArtist = saveTask(otherJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.INTERRUPTED, date);
        DiscoveryTask started = saveTask(otherJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.STARTED, null);
        DiscoveryTask album = saveTask(otherJob, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(1));
        List<String> artistIds = List.of(firstArtist.getId(), secondArtist.getId()).stream().sorted(Comparator.reverseOrder()).toList();
        List<String> expectedIds = filterType ? List.of(started.getId(), artistIds.getFirst(), artistIds.getLast(), older.getId())
                : List.of(started.getId(), album.getId(), artistIds.getFirst(), artistIds.getLast(), older.getId());
        DiscoveryTaskFilter filter = new DiscoveryTaskFilter(null, filterType ? DiscoveryTaskType.SPOTIFY_ARTIST_DATA : null);

        for (int pageIndex = 0; pageIndex < expectedIds.size(); pageIndex++) {
            Page<DiscoveryTask> page = discoveryJobService.getAllTasks(filter, PageRequest.of(pageIndex, 1));

            assertThat(page.getTotalElements()).isEqualTo(expectedIds.size());
            assertThat(page.getTotalPages()).isEqualTo(expectedIds.size());
            assertThat(page.getContent()).extracting(DiscoveryTask::getId).containsExactly(expectedIds.get(pageIndex));
        }
        assertThat(discoveryJobService.getAllTasks(new DiscoveryTaskFilter(null, DiscoveryTaskType.SPOTIFY_TOP_TRACKS), PageRequest.of(0, 30)))
                .isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"FAILED, false", "FAILED, true", "NO_RESULT, false", "NO_RESULT, true"})
    void shouldFilterAllTasksAcrossJobsBeforePagination(DiscoveryTaskFilter.Result result, boolean filterType) {
        DiscoveryJob job = saveJob();
        DiscoveryJob otherJob = saveJob();
        LocalDateTime date = LocalDateTime.of(2026, 10, 15, 12, 0);
        DiscoveryTask.Status matchingStatus = result == DiscoveryTaskFilter.Result.FAILED
                ? DiscoveryTask.Status.FAILED : DiscoveryTask.Status.COMPLETE;
        String matchingResult = result == DiscoveryTaskFilter.Result.NO_RESULT ? null : "{\"error\":\"failure\"}";
        DiscoveryTask firstArtist = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, matchingStatus, date, matchingResult);
        DiscoveryTask secondArtist = saveTask(otherJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, matchingStatus, date,
                result == DiscoveryTaskFilter.Result.FAILED ? null : matchingResult);
        DiscoveryTask album = saveTask(otherJob, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, matchingStatus, date.plusHours(1), matchingResult);
        for (DiscoveryTask.Status status : List.of(DiscoveryTask.Status.STARTED, DiscoveryTask.Status.INTERRUPTED)) {
            saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, status, date.plusHours(2), null);
        }
        saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(2), "[]");
        saveTask(otherJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(2), "{}");
        DiscoveryTask.Status otherStatus = result == DiscoveryTaskFilter.Result.FAILED
                ? DiscoveryTask.Status.COMPLETE : DiscoveryTask.Status.FAILED;
        saveTask(otherJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, otherStatus, date.plusHours(2), null);
        List<String> artistIds = List.of(firstArtist.getId(), secondArtist.getId()).stream().sorted(Comparator.reverseOrder()).toList();
        List<String> expectedIds = filterType ? artistIds : List.of(album.getId(), artistIds.getFirst(), artistIds.getLast());
        DiscoveryTaskFilter filter = new DiscoveryTaskFilter(result, filterType ? DiscoveryTaskType.SPOTIFY_ARTIST_DATA : null);

        for (int pageIndex = 0; pageIndex < expectedIds.size(); pageIndex++) {
            Page<DiscoveryTask> page = discoveryJobService.getAllTasks(filter, PageRequest.of(pageIndex, 1));

            assertThat(page.getTotalElements()).isEqualTo(expectedIds.size());
            assertThat(page.getTotalPages()).isEqualTo(expectedIds.size());
            assertThat(page.getContent()).extracting(DiscoveryTask::getId).containsExactly(expectedIds.get(pageIndex));
        }
        assertThat(discoveryJobService.getAllTasks(new DiscoveryTaskFilter(result, DiscoveryTaskType.SPOTIFY_TOP_TRACKS), PageRequest.of(0, 30)))
                .isEmpty();
    }

    @Test
    void shouldPageAllJobTasksWithStableOrderByLastActivity() {
        DiscoveryJob job = saveJob();
        DiscoveryJob otherJob = saveJob();
        LocalDateTime date = LocalDateTime.of(2026, 10, 15, 12, 0);
        DiscoveryTask older = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.minusHours(1));
        DiscoveryTask album = saveTask(job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.FAILED, date);
        DiscoveryTask topTracks = saveTask(job, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.INTERRUPTED, date);
        DiscoveryTask started = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.STARTED, null);
        saveTask(otherJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(1));
        List<String> newestIds = List.of(album.getId(), topTracks.getId()).stream().sorted(Comparator.reverseOrder()).toList();
        DiscoveryTaskFilter filter = new DiscoveryTaskFilter(null, null);

        Page<DiscoveryTask> first = discoveryJobService.getJobTasks(job.getId(), filter, PageRequest.of(0, 2));
        Page<DiscoveryTask> second = discoveryJobService.getJobTasks(job.getId(), filter, PageRequest.of(1, 2));

        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(DiscoveryTask::getId).containsExactly(started.getId(), newestIds.getFirst());
        assertThat(second.getContent()).extracting(DiscoveryTask::getId).containsExactly(newestIds.getLast(), older.getId());
        assertThat(taskRepository.countByJobId(job.getId())).isEqualTo(4);
    }

    @Test
    void shouldFilterTasksBeforePaginationAndCountOnlyMatchingRows() {
        DiscoveryJob job = saveJob();
        DiscoveryJob otherJob = saveJob();
        LocalDateTime date = LocalDateTime.of(2026, 10, 15, 12, 0);
        DiscoveryTask older = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date);
        DiscoveryTask newer = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.STARTED, date.plusHours(1));
        saveTask(job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(2));
        saveTask(otherJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(3));
        DiscoveryTaskFilter filter = new DiscoveryTaskFilter(null, DiscoveryTaskType.SPOTIFY_ARTIST_DATA);

        Page<DiscoveryTask> first = discoveryJobService.getJobTasks(job.getId(), filter, PageRequest.of(0, 1));
        Page<DiscoveryTask> second = discoveryJobService.getJobTasks(job.getId(), filter, PageRequest.of(1, 1));

        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(DiscoveryTask::getId).containsExactly(newer.getId());
        assertThat(second.getContent()).extracting(DiscoveryTask::getId).containsExactly(older.getId());
        assertThat(taskRepository.findByJobIdAndType(job.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, PageRequest.of(0, 30))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"FAILED, false", "FAILED, true", "NO_RESULT, false", "NO_RESULT, true"})
    void shouldFilterTasksByResultAndTypeBeforePagination(DiscoveryTaskFilter.Result result, boolean filterType) {
        DiscoveryJob job = saveJob();
        DiscoveryJob otherJob = saveJob();
        LocalDateTime date = LocalDateTime.of(2026, 10, 15, 12, 0);
        DiscoveryTask.Status matchingStatus = result == DiscoveryTaskFilter.Result.FAILED
                ? DiscoveryTask.Status.FAILED : DiscoveryTask.Status.COMPLETE;
        String matchingResult = result == DiscoveryTaskFilter.Result.NO_RESULT ? null : "{\"error\":\"failure\"}";
        DiscoveryTask firstArtist = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, matchingStatus, date, matchingResult);
        DiscoveryTask secondArtist = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, matchingStatus, date,
                result == DiscoveryTaskFilter.Result.FAILED ? null : matchingResult);
        DiscoveryTask album = saveTask(job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, matchingStatus, date.plusHours(1), matchingResult);
        saveTask(otherJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, matchingStatus, date.plusHours(2), matchingResult);
        for (DiscoveryTask.Status status : List.of(DiscoveryTask.Status.STARTED, DiscoveryTask.Status.INTERRUPTED)) {
            saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, status, date.plusHours(2), null);
        }
        saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(2), "[]");
        saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(2), "{}");
        DiscoveryTask.Status otherStatus = result == DiscoveryTaskFilter.Result.FAILED
                ? DiscoveryTask.Status.COMPLETE : DiscoveryTask.Status.FAILED;
        saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, otherStatus, date.plusHours(2), null);
        List<String> artistIds = List.of(firstArtist.getId(), secondArtist.getId()).stream().sorted(Comparator.reverseOrder()).toList();
        List<String> expectedIds = filterType ? artistIds : List.of(album.getId(), artistIds.getFirst(), artistIds.getLast());
        DiscoveryTaskType type = filterType ? DiscoveryTaskType.SPOTIFY_ARTIST_DATA : null;
        DiscoveryTaskFilter filter = new DiscoveryTaskFilter(result, type);

        for (int pageIndex = 0; pageIndex < expectedIds.size(); pageIndex++) {
            Page<DiscoveryTask> page = discoveryJobService.getJobTasks(job.getId(), filter, PageRequest.of(pageIndex, 1));

            assertThat(page.getTotalElements()).isEqualTo(expectedIds.size());
            assertThat(page.getTotalPages()).isEqualTo(expectedIds.size());
            assertThat(page.getContent()).extracting(DiscoveryTask::getId).containsExactly(expectedIds.get(pageIndex));
        }
        assertThat(discoveryJobService.getJobTasks(job.getId(), new DiscoveryTaskFilter(result, DiscoveryTaskType.SPOTIFY_TOP_TRACKS), PageRequest.of(0, 30)))
                .isEmpty();
    }

    private DiscoveryJob saveJob() {
        return jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.STARTED));
    }

    private DiscoveryTask saveTask(DiscoveryJob job, DiscoveryTaskType type, DiscoveryTask.Status status, LocalDateTime updateDate) {
        return saveTask(job, type, status, updateDate, null);
    }

    private DiscoveryTask saveTask(DiscoveryJob job, DiscoveryTaskType type, DiscoveryTask.Status status, LocalDateTime updateDate, String result) {
        DiscoveryTask task = taskRepository.save(new DiscoveryTask().setJob(job).setType(type).setStatus(status).setResult(result));
        if (updateDate != null) {
            jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", Timestamp.valueOf(updateDate), task.getId());
        }
        return task;
    }
}
