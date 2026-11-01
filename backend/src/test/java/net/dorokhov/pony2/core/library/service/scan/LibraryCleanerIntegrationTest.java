package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.File;
import java.io.IOException;

import static net.dorokhov.pony2.test.ExternalImageFixtures.saveExternalImage;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LibraryCleanerIntegrationTest extends IntegrationTest {

    @Autowired
    private LibraryCleaner libraryCleaner;

    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Autowired
    private DiscoveryJobRepository discoveryJobRepository;
    @Autowired
    private DiscoveryTaskRepository discoveryTaskRepository;
    @Autowired
    private ExternalImageRepository externalImageRepository;
    @Value("${pony.externalImage.path}")
    private File externalImageFolder;

    @Test
    void shouldDeleteBothExternalImagesAndFilesWithArtist() throws IOException {
        Artist artist = artistRepository.save(new Artist());
        ExternalImageFile bio = saveExternalImage(externalImageRepository, externalImageFolder, "image.png");
        ExternalImageFile avatar = saveExternalImage(externalImageRepository, externalImageFolder, "green.png");
        artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(artist)
                .setBioExternalImage(bio.getExternalImage()).setAvatarExternalImage(avatar.getExternalImage()));

        assertThat(libraryCleaner.deleteArtistIfUnused(artist)).isTrue();

        assertThat(externalImageRepository.count()).isZero();
        assertThat(bio.getFile()).doesNotExist();
        assertThat(avatar.getFile()).doesNotExist();
    }

    @Test
    void shouldRejectAccidentalExternalImageReuseAndKeepFile() throws IOException {
        Artist first = artistRepository.save(new Artist());
        Artist second = artistRepository.save(new Artist());
        ExternalImageFile externalImageFile = saveExternalImage(externalImageRepository, externalImageFolder, "image.png");
        ExternalImage externalImage = externalImageFile.getExternalImage();
        artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(first)
                .setBioExternalImage(externalImage));
        artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(second).setAvatarExternalImage(externalImage));

        assertThatThrownBy(() -> libraryCleaner.deleteArtistIfUnused(first))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(artistRepository.existsById(first.getId())).isTrue();
        assertThat(artistDiscoveryRepository.findByArtistId(first.getId())).isPresent();
        assertThat(artistDiscoveryRepository.findByArtistId(second.getId())).isPresent();
        assertThat(externalImageRepository.existsById(externalImage.getId())).isTrue();
        assertThat(externalImageFile.getFile()).exists();
    }

    @Test
    void shouldKeepExternalImageAndFileWhenArtistDeletionIsRolledBack() throws IOException {
        Artist artist = artistRepository.save(new Artist());
        ExternalImageFile externalImageFile = saveExternalImage(externalImageRepository, externalImageFolder, "image.png");
        artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(artist)
                .setBioExternalImage(externalImageFile.getExternalImage()));

        getTransactionTemplate().executeWithoutResult(status -> {
            assertThat(libraryCleaner.deleteArtistIfUnused(artist)).isTrue();
            assertThat(externalImageFile.getFile()).exists();
            status.setRollbackOnly();
        });

        assertThat(artistRepository.existsById(artist.getId())).isTrue();
        assertThat(artistDiscoveryRepository.findByArtistId(artist.getId())).isPresent();
        assertThat(externalImageRepository.existsById(externalImageFile.getExternalImage().getId())).isTrue();
        assertThat(externalImageFile.getFile()).exists();
    }

    @Test
    public void shouldDeleteArtistWithDiscovery() {
        Artist artist = artistRepository.save(new Artist());
        DiscoveryJob job = discoveryJobRepository.save(saveDiscoveryJob().setArtist(artist));
        DiscoveryTask task = discoveryTaskRepository.saveAndFlush(
                new DiscoveryTask().setArtist(artist).setJob(job)
                        .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA).setStatus(DiscoveryTask.Status.COMPLETE)
                        .setResult("{}"));
        artistDiscoveryRepository.saveAndFlush(new ArtistDiscovery()
                .setArtist(artist).setSpotifyArtistId("spotify-artist"));

        assertThat(libraryCleaner.deleteArtistIfUnused(artist)).isTrue();

        assertThat(artistRepository.findById(artist.getId())).isEmpty();
        assertThat(artistDiscoveryRepository.count()).isZero();
        assertThat(discoveryJobRepository.findById(job.getId()).orElseThrow().getArtist()).isNull();
        assertThat(discoveryTaskRepository.count()).isOne();
        assertThat(discoveryTaskRepository.findById(task.getId()).orElseThrow().getArtist()).isNull();
    }

    @Test
    public void shouldKeepDiscoveryIndexWhenArtistStillHasAlbums() {
        Artist artist = artistRepository.save(new Artist());
        albumRepository.save(new Album().setArtist(artist));
        ArtistDiscovery index = artistDiscoveryRepository.save(new ArtistDiscovery()
                .setArtist(artist)
                .setSpotifyArtistId("spotify-artist"));

        assertThat(libraryCleaner.deleteArtistIfUnused(artist)).isFalse();

        assertThat(artistDiscoveryRepository.findById(index.getId())).isNotEmpty();
        assertThat(artistRepository.findById(artist.getId())).isNotEmpty();
    }

    @Test
    public void shouldDeleteAlbumWithDiscovery() {
        Artist artist = artistRepository.save(new Artist());
        Album album = albumRepository.save(new Album().setArtist(artist));
        DiscoveryJob job = discoveryJobRepository.save(saveDiscoveryJob().setType(DiscoveryType.ALBUM).setAlbum(album));
        DiscoveryTask task = discoveryTaskRepository.saveAndFlush(
                new DiscoveryTask().setAlbum(album).setJob(job)
                        .setType(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA).setStatus(DiscoveryTask.Status.COMPLETE)
                        .setResult("{}"));

        assertThat(libraryCleaner.deleteAlbumIfUnused(album)).isTrue();

        assertThat(albumRepository.findById(album.getId())).isEmpty();
        assertThat(discoveryJobRepository.findById(job.getId()).orElseThrow().getAlbum()).isNull();
        assertThat(discoveryTaskRepository.count()).isOne();
        assertThat(discoveryTaskRepository.findById(task.getId()).orElseThrow().getAlbum()).isNull();
    }

    @Test
    public void shouldDeleteGenreWithDiscovery() {
        Genre genre = genreRepository.save(new Genre());
        DiscoveryJob job = discoveryJobRepository.save(saveDiscoveryJob().setType(DiscoveryType.GENRE).setGenre(genre));
        DiscoveryTask task = discoveryTaskRepository.saveAndFlush(new DiscoveryTask().setGenre(genre).setJob(job)
                .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA).setStatus(DiscoveryTask.Status.COMPLETE).setResult("{}"));

        assertThat(libraryCleaner.deleteGenreIfUnused(genre)).isTrue();

        assertThat(genreRepository.findById(genre.getId())).isEmpty();
        assertThat(discoveryJobRepository.findById(job.getId()).orElseThrow().getGenre()).isNull();
        assertThat(discoveryTaskRepository.findById(task.getId()).orElseThrow().getGenre()).isNull();
        assertThat(discoveryTaskRepository.findById(task.getId()).orElseThrow().getResult()).isEqualTo("{}");
    }

    @ParameterizedTest
    @EnumSource(value = DiscoveryType.class, names = {"ARTIST", "ALBUM", "GENRE"})
    public void shouldKeepDiscoveryTargetsWhenStillInUse(DiscoveryType type) {
        Artist artist = artistRepository.save(new Artist());
        Album album = albumRepository.save(new Album().setArtist(artist));
        Genre genre = genreRepository.save(new Genre());
        songRepository.save(song().setAlbum(album).setGenre(genre));
        DiscoveryJob job = new DiscoveryJob().setType(type).setStatus(DiscoveryJob.Status.COMPLETE);
        DiscoveryTask task = new DiscoveryTask().setJob(job).setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .setStatus(DiscoveryTask.Status.COMPLETE).setResult("{}");
        switch (type) {
            case ARTIST -> {
                job.setArtist(artist);
                task.setArtist(artist);
            }
            case ALBUM -> {
                job.setAlbum(album);
                task.setAlbum(album);
            }
            case GENRE -> {
                job.setGenre(genre);
                task.setGenre(genre);
            }
            default -> throw new IllegalArgumentException("Unexpected test type.");
        }
        discoveryJobRepository.saveAndFlush(job);
        discoveryTaskRepository.saveAndFlush(task);

        boolean deleted = switch (type) {
            case ARTIST -> libraryCleaner.deleteArtistIfUnused(artist);
            case ALBUM -> libraryCleaner.deleteAlbumIfUnused(album);
            case GENRE -> libraryCleaner.deleteGenreIfUnused(genre);
            default -> throw new IllegalArgumentException("Unexpected test type.");
        };

        assertThat(deleted).isFalse();
        DiscoveryJob savedJob = discoveryJobRepository.findById(job.getId()).orElseThrow();
        DiscoveryTask savedTask = discoveryTaskRepository.findById(task.getId()).orElseThrow();
        switch (type) {
            case ARTIST -> {
                assertThat(savedJob.getArtist().getId()).isEqualTo(artist.getId());
                assertThat(savedTask.getArtist().getId()).isEqualTo(artist.getId());
            }
            case ALBUM -> {
                assertThat(savedJob.getAlbum().getId()).isEqualTo(album.getId());
                assertThat(savedTask.getAlbum().getId()).isEqualTo(album.getId());
            }
            case GENRE -> {
                assertThat(savedJob.getGenre().getId()).isEqualTo(genre.getId());
                assertThat(savedTask.getGenre().getId()).isEqualTo(genre.getId());
            }
            default -> throw new IllegalArgumentException("Unexpected test type.");
        }
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
