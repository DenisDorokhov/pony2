package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import org.junit.jupiter.api.Test;
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
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPageAllJobTasksByUpdateDateWithStableOrderAndNullsLast() {
        DiscoveryJob job = saveJob();
        DiscoveryJob otherJob = saveJob();
        LocalDateTime date = LocalDateTime.of(2026, 10, 15, 12, 0);
        DiscoveryTask older = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.minusHours(1));
        DiscoveryTask album = saveTask(job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.FAILED, date);
        DiscoveryTask topTracks = saveTask(job, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.INTERRUPTED, date);
        DiscoveryTask started = saveTask(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.STARTED, null);
        saveTask(otherJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE, date.plusHours(1));
        List<String> newestIds = List.of(album.getId(), topTracks.getId()).stream().sorted(Comparator.reverseOrder()).toList();

        Page<DiscoveryTask> first = taskRepository.findByJobIdAndType(job.getId(), null, PageRequest.of(0, 2));
        Page<DiscoveryTask> second = taskRepository.findByJobIdAndType(job.getId(), null, PageRequest.of(1, 2));

        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(DiscoveryTask::getId).containsExactlyElementsOf(newestIds);
        assertThat(second.getContent()).extracting(DiscoveryTask::getId).containsExactly(older.getId(), started.getId());
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

        Page<DiscoveryTask> first = taskRepository.findByJobIdAndType(job.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, PageRequest.of(0, 1));
        Page<DiscoveryTask> second = taskRepository.findByJobIdAndType(job.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, PageRequest.of(1, 1));

        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(DiscoveryTask::getId).containsExactly(newer.getId());
        assertThat(second.getContent()).extracting(DiscoveryTask::getId).containsExactly(older.getId());
        assertThat(taskRepository.findByJobIdAndType(job.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, PageRequest.of(0, 30))).isEmpty();
    }

    private DiscoveryJob saveJob() {
        return jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.STARTED));
    }

    private DiscoveryTask saveTask(DiscoveryJob job, DiscoveryTaskType type, DiscoveryTask.Status status, LocalDateTime updateDate) {
        DiscoveryTask task = taskRepository.save(new DiscoveryTask().setJob(job).setType(type).setStatus(status));
        if (updateDate != null) {
            jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", Timestamp.valueOf(updateDate), task.getId());
        }
        return task;
    }
}
