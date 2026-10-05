package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LibraryCleanerIntegrationTest extends IntegrationTest {

    @Autowired
    private LibraryCleaner libraryCleaner;

    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Autowired
    private AlbumDiscoveryRepository albumDiscoveryRepository;
    @Autowired
    private DiscoveryJobRepository discoveryJobRepository;
    @Autowired
    private DiscoveryTaskRepository discoveryTaskRepository;

    @Test
    public void shouldDeleteArtistWithDiscovery() {

        getTransactionTemplate().execute(status -> {

            DiscoveryTask task = saveDiscoveryTask(saveDiscoveryJob());
            Artist artist = new Artist();
            artist.getDiscoveries().add(new ArtistDiscovery()
                    .setArtist(artist)
                    .setJob(task.getJob())
                    .setTasks(List.of(task)));
            artist = artistRepository.save(artist);
            artistRepository.flush();

            assertThat(libraryCleaner.deleteArtistIfUnused(artist)).isTrue();
            artistRepository.flush();

            assertThat(artistRepository.findById(artist.getId())).isEmpty();
            assertThat(artistDiscoveryRepository.count()).isZero();
            assertThat(discoveryTaskRepository.count()).isOne();

            return null;
        });
    }

    @Test
    public void shouldDeleteAlbumWithDiscovery() {

        getTransactionTemplate().execute(status -> {

            Artist artist = artistRepository.save(new Artist());
            DiscoveryTask task = saveDiscoveryTask(saveDiscoveryJob());
            Album album = new Album().setArtist(artist);
            album.getDiscoveries().add(new AlbumDiscovery()
                    .setAlbum(album)
                    .setJob(task.getJob())
                    .setTasks(List.of(task)));
            album = albumRepository.save(album);
            albumRepository.flush();

            assertThat(libraryCleaner.deleteAlbumIfUnused(album)).isTrue();
            albumRepository.flush();

            assertThat(albumRepository.findById(album.getId())).isEmpty();
            assertThat(albumDiscoveryRepository.count()).isZero();
            assertThat(discoveryTaskRepository.count()).isOne();

            return null;
        });
    }

    private DiscoveryJob saveDiscoveryJob() {
        return discoveryJobRepository.save(new DiscoveryJob()
                .setType(DiscoveryType.ARTIST)
                .setStatus(DiscoveryJob.Status.COMPLETE));
    }

    private DiscoveryTask saveDiscoveryTask(DiscoveryJob job) {
        return discoveryTaskRepository.save(new DiscoveryTask()
                .setJob(job)
                .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATE)
                .setStatus(DiscoveryTask.Status.COMPLETE)
                .setArgument("{}")
                .setResult("{}"));
    }
}
