package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.service.scan.BatchLibraryCleaner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscoveryTaskTargetRepositoryTest extends IntegrationTest {

    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private DiscoveryTaskRepository taskRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private SongRepository songRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private BatchLibraryCleaner batchLibraryCleaner;

    @Test
    void shouldEnforceSingleTargetAndKeepHistoryAfterDeletingEveryTargetKind() {
        Artist artist = artistRepository.save(new Artist().setName("Artist"));
        Album album = albumRepository.save(new Album().setArtist(artist).setName("Album"));
        Genre genre = genreRepository.save(new Genre().setName("Genre"));
        Song song = songRepository.save(song().setAlbum(album).setGenre(genre));
        DiscoveryJob job = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.COMPLETE));
        DiscoveryTask artistTask = taskRepository.save(task(job).setArtist(artist));
        DiscoveryTask albumTask = taskRepository.save(task(job).setAlbum(album));
        DiscoveryTask genreTask = taskRepository.save(task(job).setGenre(genre));
        DiscoveryTask songTask = taskRepository.save(task(job).setSong(song));

        assertThatThrownBy(() -> taskRepository.saveAndFlush(task(job).setArtist(artist).setAlbum(album)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM song WHERE id = ?", song.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        batchLibraryCleaner.cleanSongs(List.of(), (itemsComplete, itemsTotal) -> {});

        assertThat(songRepository.count()).isZero();
        assertThat(albumRepository.count()).isZero();
        assertThat(artistRepository.count()).isZero();
        assertThat(genreRepository.count()).isZero();
        assertThat(taskRepository.count()).isEqualTo(4);
        assertThat(taskRepository.findById(artistTask.getId()).orElseThrow().getArtist()).isNull();
        assertThat(taskRepository.findById(albumTask.getId()).orElseThrow().getAlbum()).isNull();
        assertThat(taskRepository.findById(genreTask.getId()).orElseThrow().getGenre()).isNull();
        assertThat(taskRepository.findById(songTask.getId()).orElseThrow().getSong()).isNull();
        assertThat(taskRepository.findAll()).allSatisfy(task -> assertThat(task.getResult()).isEqualTo("{}"));
    }

    private DiscoveryTask task(DiscoveryJob job) {
        return new DiscoveryTask().setJob(job).setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .setStatus(DiscoveryTask.Status.COMPLETE).setResult("{}");
    }
}
