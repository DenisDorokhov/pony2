package net.dorokhov.pony2.core.library.repository;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryTaskHistoryRepositoryTest extends IntegrationTest {

    @Autowired private DiscoveryTaskRepository taskRepository;
    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private DiscoveryJobService discoveryJobService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void shouldIncludeArtistAndAllAlbumsAcrossJobsWithStableOrderAndNullsLast() {
        Artist artist = artistRepository.save(new Artist().setName("Artist"));
        Artist otherArtist = artistRepository.save(new Artist().setName("Other artist"));
        Album firstAlbum = albumRepository.save(new Album().setArtist(artist).setName("First album"));
        Album secondAlbum = albumRepository.save(new Album().setArtist(artist).setName("Second album"));
        Album otherAlbum = albumRepository.save(new Album().setArtist(otherArtist).setName("Other album"));
        DiscoveryJob fullJob = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.COMPLETE));
        DiscoveryJob albumJob = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.ALBUM)
                .setAlbum(firstAlbum).setStatus(DiscoveryJob.Status.STARTED));
        LocalDateTime date = LocalDateTime.of(2026, 10, 22, 8, 0);
        DiscoveryTask ownTask = saveTask(task(fullJob).setArtist(artist), date.minusHours(1));
        DiscoveryTask firstAlbumTask = saveTask(task(fullJob).setAlbum(firstAlbum), date);
        DiscoveryTask secondAlbumTask = saveTask(task(albumJob).setAlbum(secondAlbum), date);
        DiscoveryTask runningTask = saveTask(task(albumJob).setArtist(artist).setStatus(DiscoveryTask.Status.STARTED), null);
        saveTask(task(fullJob).setArtist(otherArtist), date.plusHours(1));
        saveTask(task(fullJob).setAlbum(otherAlbum), date.plusHours(1));
        saveTask(task(fullJob), date.plusHours(1));
        List<String> tiedIds = List.of(firstAlbumTask.getId(), secondAlbumTask.getId()).stream()
                .sorted(Comparator.reverseOrder()).toList();
        DiscoveryTaskFilter filter = new DiscoveryTaskFilter(null, null);

        Page<DiscoveryTask> first = discoveryJobService.getArtistTasks(artist.getId(), filter, PageRequest.of(0, 2));
        Page<DiscoveryTask> second = discoveryJobService.getArtistTasks(artist.getId(), filter, PageRequest.of(1, 2));

        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(DiscoveryTask::getId).containsExactlyElementsOf(tiedIds);
        assertThat(second.getContent()).extracting(DiscoveryTask::getId).containsExactly(ownTask.getId(), runningTask.getId());
        assertThat(discoveryJobService.getArtistTasks("missing", filter, PageRequest.of(0, 30))).isEmpty();
        assertThat(discoveryJobService.getAlbumTasks("missing", filter, PageRequest.of(0, 30))).isEmpty();
    }

    private DiscoveryTask task(DiscoveryJob job) {
        return new DiscoveryTask().setJob(job).setType(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA)
                .setStatus(DiscoveryTask.Status.COMPLETE);
    }

    private DiscoveryTask saveTask(DiscoveryTask task, @Nullable LocalDateTime updateDate) {
        DiscoveryTask saved = taskRepository.save(task);
        jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?",
                updateDate != null ? Timestamp.valueOf(updateDate) : null, saved.getId());
        return saved;
    }

    @ParameterizedTest
    @CsvSource({
            "ARTIST,,false", "ARTIST,,true", "ALBUM,,false", "ALBUM,,true",
            "ARTIST,FAILED,false", "ARTIST,FAILED,true", "ALBUM,FAILED,false", "ALBUM,FAILED,true",
            "ARTIST,NO_RESULT,false", "ARTIST,NO_RESULT,true", "ALBUM,NO_RESULT,false", "ALBUM,NO_RESULT,true"
    })
    void shouldFilterHistoryBeforePaginationAcrossJobs(DiscoveryType scope, @Nullable DiscoveryTaskFilter.Result result, boolean filterType) {
        Artist artist = artistRepository.save(new Artist().setName("Artist"));
        Artist otherArtist = artistRepository.save(new Artist().setName("Other artist"));
        Album album = albumRepository.save(new Album().setArtist(artist).setName("Album"));
        Album otherAlbum = albumRepository.save(new Album().setArtist(otherArtist).setName("Other album"));
        DiscoveryJob firstJob = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.COMPLETE));
        DiscoveryJob secondJob = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.ARTIST)
                .setArtist(artist).setStatus(DiscoveryJob.Status.FAILED));
        DiscoveryTask.Status matchingStatus = result == DiscoveryTaskFilter.Result.FAILED
                ? DiscoveryTask.Status.FAILED : DiscoveryTask.Status.COMPLETE;
        String matchingResult = result == DiscoveryTaskFilter.Result.NO_RESULT ? "null" : "{}";
        LocalDateTime date = LocalDateTime.of(2026, 10, 22, 8, 0);
        DiscoveryTask artistTask = saveTask(task(firstJob).setArtist(artist).setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .setStatus(matchingStatus).setResult(matchingResult), date);
        DiscoveryTask olderAlbumTask = saveTask(task(firstJob).setAlbum(album)
                .setStatus(matchingStatus).setResult(matchingResult), date.plusMinutes(1));
        DiscoveryTask newerAlbumTask = saveTask(task(secondJob).setAlbum(album)
                .setStatus(matchingStatus).setResult(matchingResult), date.plusMinutes(2));
        saveTask(task(firstJob).setArtist(otherArtist).setStatus(matchingStatus).setResult(matchingResult), date.plusHours(1));
        saveTask(task(firstJob).setAlbum(otherAlbum).setStatus(matchingStatus).setResult(matchingResult), date.plusHours(1));
        if (result != null) {
            for (DiscoveryTask.Status status : List.of(DiscoveryTask.Status.STARTED, DiscoveryTask.Status.INTERRUPTED)) {
                saveTask(task(firstJob).setAlbum(album).setStatus(status).setResult("null"), date.plusHours(1));
            }
            saveTask(task(firstJob).setAlbum(album).setStatus(DiscoveryTask.Status.COMPLETE).setResult("{}"), date.plusHours(1));
            saveTask(task(firstJob).setAlbum(album).setStatus(DiscoveryTask.Status.COMPLETE), date.plusHours(1));
            if (result == DiscoveryTaskFilter.Result.NO_RESULT) {
                saveTask(task(firstJob).setAlbum(album).setStatus(DiscoveryTask.Status.FAILED).setResult("null"), date.plusHours(1));
            }
        }
        List<String> expectedIds = new ArrayList<>(List.of(newerAlbumTask.getId(), olderAlbumTask.getId()));
        if (scope == DiscoveryType.ARTIST && !filterType) {
            expectedIds.add(artistTask.getId());
        }
        DiscoveryTaskFilter filter = new DiscoveryTaskFilter(result, filterType ? DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA : null);

        for (int pageIndex = 0; pageIndex < expectedIds.size(); pageIndex++) {
            PageRequest pageRequest = PageRequest.of(pageIndex, 1);
            Page<DiscoveryTask> page = scope == DiscoveryType.ARTIST
                    ? discoveryJobService.getArtistTasks(artist.getId(), filter, pageRequest)
                    : discoveryJobService.getAlbumTasks(album.getId(), filter, pageRequest);

            assertThat(page.getTotalElements()).isEqualTo(expectedIds.size());
            assertThat(page.getTotalPages()).isEqualTo(expectedIds.size());
            assertThat(page.getContent()).extracting(DiscoveryTask::getId).containsExactly(expectedIds.get(pageIndex));
        }
    }
}
