package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LibraryCleanerIntegrationTest extends IntegrationTest {

    @Autowired
    private LibraryCleaner libraryCleaner;

    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository;
    @Autowired
    private DiscoveryJobRepository discoveryJobRepository;
    @Autowired
    private DiscoveryTaskRepository discoveryTaskRepository;

    @Test
    public void shouldDeleteArtistWithDiscovery() {
        Artist artist = artistRepository.save(new Artist());
        DiscoveryTask task = discoveryTaskRepository.saveAndFlush(
                new DiscoveryTask().setArtist(artist).setJob(saveDiscoveryJob())
                        .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA).setStatus(DiscoveryTask.Status.COMPLETE)
                        .setResult("{}"));
        artistDiscoveryIndexRepository.saveAndFlush(new ArtistDiscoveryIndex()
                .setArtist(artist).setSpotifyArtistId("spotify-artist"));

        assertThat(libraryCleaner.deleteArtistIfUnused(artist)).isTrue();

        assertThat(artistRepository.findById(artist.getId())).isEmpty();
        assertThat(artistDiscoveryIndexRepository.count()).isZero();
        assertThat(discoveryTaskRepository.count()).isOne();
        assertThat(discoveryTaskRepository.findById(task.getId()).orElseThrow().getArtist()).isNull();
    }

    @Test
    public void shouldKeepDiscoveryIndexWhenArtistStillHasAlbums() {
        Artist artist = artistRepository.save(new Artist());
        albumRepository.save(new Album().setArtist(artist));
        ArtistDiscoveryIndex index = artistDiscoveryIndexRepository.save(new ArtistDiscoveryIndex()
                .setArtist(artist)
                .setSpotifyArtistId("spotify-artist"));

        assertThat(libraryCleaner.deleteArtistIfUnused(artist)).isFalse();

        assertThat(artistDiscoveryIndexRepository.findById(index.getId())).isNotEmpty();
        assertThat(artistRepository.findById(artist.getId())).isNotEmpty();
    }

    @Test
    public void shouldDeleteAlbumWithDiscovery() {
        Artist artist = artistRepository.save(new Artist());
        Album album = albumRepository.save(new Album().setArtist(artist));
        DiscoveryTask task = discoveryTaskRepository.saveAndFlush(
                new DiscoveryTask().setAlbum(album).setJob(saveDiscoveryJob())
                        .setType(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA).setStatus(DiscoveryTask.Status.COMPLETE)
                        .setResult("{}"));

        assertThat(libraryCleaner.deleteAlbumIfUnused(album)).isTrue();

        assertThat(albumRepository.findById(album.getId())).isEmpty();
        assertThat(discoveryTaskRepository.count()).isOne();
        assertThat(discoveryTaskRepository.findById(task.getId()).orElseThrow().getAlbum()).isNull();
    }

    private DiscoveryJob saveDiscoveryJob() {
        return discoveryJobRepository.save(new DiscoveryJob()
                .setType(DiscoveryType.ARTIST)
                .setStatus(DiscoveryJob.Status.COMPLETE));
    }

    private DiscoveryTask saveDiscoveryTask(DiscoveryJob job) {
        return discoveryTaskRepository.save(new DiscoveryTask()
                .setJob(job)
                .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .setStatus(DiscoveryTask.Status.COMPLETE)
                .setResult("{}"));
    }
}
