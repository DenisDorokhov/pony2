package net.dorokhov.pony3;

import com.google.common.io.Files;
import net.dorokhov.pony3.api.library.domain.*;
import net.dorokhov.pony3.api.library.service.ScanJobService;
import net.dorokhov.pony3.api.log.domain.LogMessage;
import net.dorokhov.pony3.core.library.repository.ArtistRepository;
import net.dorokhov.pony3.core.library.repository.GenreRepository;
import net.dorokhov.pony3.core.library.repository.SongRepository;
import net.dorokhov.pony3.core.library.service.AudioTagger;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;

@Component
public class ScanTestPlanExecutor {

    public static final class Context {

        private final ScanTestPlan scanTestPlan;
        private final File rootFolder;

        private Context(ScanTestPlan scanTestPlan, File rootFolder) {
            this.scanTestPlan = checkNotNull(scanTestPlan);
            this.rootFolder = checkNotNull(rootFolder);
        }

        public ScanTestPlan getScanTestPlan() {
            return scanTestPlan;
        }

        public File getRootFolder() {
            return rootFolder;
        }
    }

    private static final Resource AUDIO_TEMPLATE = new ClassPathResource("empty.mp3");

    private final AudioTagger audioTagger;
    private final ScanJobService scanJobService;
    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;

    private final File rootFolder;

    public ScanTestPlanExecutor(
            AudioTagger audioTagger, ScanJobService scanJobService,
            GenreRepository genreRepository, ArtistRepository artistRepository, SongRepository songRepository
    ) {
        this.audioTagger = audioTagger;
        this.scanJobService = scanJobService;
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.songRepository = songRepository;

        rootFolder = Files.createTempDir();
    }

    public Context prepare(ScanTestPlan scanTestPlan) throws IOException {
        for (ScanTestPlan.FileArtwork fileArtwork : scanTestPlan.getArtworksToGenerate()) {
            File targetFile = new File(rootFolder, fileArtwork.getCopyTo());
            Files.createParentDirs(targetFile);
            Files.copy(new ClassPathResource(fileArtwork.getCopyFrom()).getFile(), targetFile);
        }
        for (ScanTestPlan.SongToGenerate songToGenerate : scanTestPlan.getSongsToGenerate()) {
            File targetFile = new File(rootFolder, songToGenerate.getPath());
            Files.createParentDirs(targetFile);
            Files.copy(AUDIO_TEMPLATE.getFile(), targetFile);
            writeAudioData(targetFile, songToGenerate);
        }
        for (String filePath : scanTestPlan.getFilePathsToDelete()) {
            File file = new File(rootFolder, filePath);
            java.nio.file.Files.delete(file.toPath());
        }
        return new Context(scanTestPlan, rootFolder);
    }

    @Transactional(readOnly = true)
    public void verify(String scanJobId, Context context) {
        verifyScanJob(scanJobService.getById(scanJobId).orElse(null), context);
        verifyGenres(context);
        verifyArtists(context);
        verifySongs(context);
    }

    public void clean() {
        FileSystemUtils.deleteRecursively(rootFolder);
    }

    private void writeAudioData(File file, ScanTestPlan.SongToGenerate songToGenerate) throws IOException {
        audioTagger.write(file, new WritableAudioData()
                .setDiscNumber(songToGenerate.getDiscNumber())
                .setDiscCount(songToGenerate.getDiscCount())
                .setTrackNumber(songToGenerate.getTrackNumber())
                .setTrackCount(songToGenerate.getTrackCount())
                .setTitle(songToGenerate.getTitle())
                .setArtist(songToGenerate.getArtist())
                .setAlbumArtist(songToGenerate.getAlbumArtist())
                .setAlbum(songToGenerate.getAlbum())
                .setYear(songToGenerate.getYear())
                .setGenre(songToGenerate.getGenre())
                .setArtworkFile(Optional.ofNullable(songToGenerate.getEmbeddedArtwork())
                        .map(ScanTestPlan.EmbeddedArtwork::getCopyFrom)
                        .map(path -> {
                            try {
                                return new ClassPathResource(path).getFile();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .orElse(null)));
    }

    private void verifyScanJob(ScanJob scanJob, Context context) {
        assertThat(scanJob).isNotNull();
        assertThat(scanJob.getCreationDate()).isNotNull();
        assertThat(scanJob.getUpdateDate()).isNotNull();
        assertThat(scanJob.getScanType()).isSameAs(ScanType.FULL);
        assertThat(scanJob.getStatus()).isSameAs(ScanJob.Status.COMPLETE);
        assertThat(scanJob.getTargetPaths()).containsExactly(context.getRootFolder().getAbsolutePath());
        assertThat(scanJob.getLogMessage()).satisfies(logMessage ->
                assertThat(logMessage.getLevel()).isSameAs(LogMessage.Level.INFO));
        assertThat(scanJob.getScanResult()).satisfies(scanResult -> {
            assertThat(scanResult.getDate()).isNotNull();
            assertThat(scanResult.getScanType()).isSameAs(ScanType.FULL);
            assertThat(scanResult.getFailedPaths()).isEmpty();
            assertThat(scanResult.getDuration()).isGreaterThan(0);
            assertThat(scanResult.getSongSize()).isGreaterThan(0);
            assertThat(scanResult.getArtworkSize()).isGreaterThan(0);
            ScanTestPlan.ExpectedData.Result expectedResult = context.getScanTestPlan().getExpectedData().getResult();
            assertThat(scanResult.getProcessedAudioFileCount()).isEqualTo(expectedResult.getProcessedAudioFileCount());
            assertThat(scanResult.getGenreCount()).isEqualTo(expectedResult.getGenreCount());
            assertThat(scanResult.getArtistCount()).isEqualTo(expectedResult.getArtistCount());
            assertThat(scanResult.getAlbumCount()).isEqualTo(expectedResult.getAlbumCount());
            assertThat(scanResult.getSongCount()).isEqualTo(expectedResult.getSongCount());
            assertThat(scanResult.getArtworkCount()).isEqualTo(expectedResult.getArtworkCount());
            assertThat(scanResult.getCreatedArtistCount()).isEqualTo(expectedResult.getCreatedArtistCount());
            assertThat(scanResult.getUpdatedArtistCount()).isEqualTo(expectedResult.getUpdatedArtistCount());
            assertThat(scanResult.getDeletedArtistCount()).isEqualTo(expectedResult.getDeletedArtistCount());
            assertThat(scanResult.getCreatedAlbumCount()).isEqualTo(expectedResult.getCreatedAlbumCount());
            assertThat(scanResult.getUpdatedAlbumCount()).isEqualTo(expectedResult.getUpdatedAlbumCount());
            assertThat(scanResult.getDeletedAlbumCount()).isEqualTo(expectedResult.getDeletedAlbumCount());
            assertThat(scanResult.getCreatedGenreCount()).isEqualTo(expectedResult.getCreatedGenreCount());
            assertThat(scanResult.getUpdatedGenreCount()).isEqualTo(expectedResult.getUpdatedGenreCount());
            assertThat(scanResult.getDeletedGenreCount()).isEqualTo(expectedResult.getDeletedGenreCount());
            assertThat(scanResult.getCreatedSongCount()).isEqualTo(expectedResult.getCreatedSongCount());
            assertThat(scanResult.getUpdatedSongCount()).isEqualTo(expectedResult.getUpdatedSongCount());
            assertThat(scanResult.getDeletedSongCount()).isEqualTo(expectedResult.getDeletedSongCount());
            assertThat(scanResult.getCreatedArtworkCount()).isEqualTo(expectedResult.getCreatedArtworkCount());
            assertThat(scanResult.getDeletedArtworkCount()).isEqualTo(expectedResult.getDeletedArtworkCount());
        });
    }

    private void verifyGenres(Context context) {
        List<Genre> genres = genreRepository.findAll();
        assertThat(genres).hasSize(context.getScanTestPlan().getExpectedData().getGenres().size());
        for (ScanTestPlan.ExpectedData.Genre expectedGenre : context.getScanTestPlan().getExpectedData().getGenres()) {
            List<Genre> foundGenres = genres.stream()
                    .filter(genre -> Objects.equals(genre.getName(), expectedGenre.getName()))
                    .toList();
            assertThat(foundGenres).hasSize(1);
            assertThat(foundGenres).first().satisfies(genre -> {
                if (expectedGenre.getArtworkPath() != null) {
                    assertThat(genre.getArtwork()).satisfies(artwork ->
                            assertThat(artwork.getSourceUri())
                                    .isEqualTo(expectedArtworkPathToUri(expectedGenre.getArtworkPath(), context)));
                } else {
                    assertThat(genre.getArtwork()).isNull();
                }
                List<Song> songs = genre.getSongs();
                assertThat(songs).hasSize(expectedGenre.getSongPaths().size());
                assertThat(songs.stream().map(Song::getPath))
                        .containsAll(expectedGenre.getSongPaths().stream()
                                .map(relativePath -> new File(context.getRootFolder().getAbsolutePath(), relativePath))
                                .map(File::getAbsolutePath)
                                .toList());
            });
        }
    }

    private void verifyArtists(Context context) {
        List<Artist> artists = artistRepository.findAll();
        assertThat(artists).hasSize(context.getScanTestPlan().getExpectedData().getArtists().size());
        for (ScanTestPlan.ExpectedData.Artist expectedArtist : context.getScanTestPlan().getExpectedData().getArtists()) {
            List<Artist> foundArtists = artists.stream()
                    .filter(artist -> Objects.equals(artist.getName(), expectedArtist.getName()))
                    .toList();
            assertThat(foundArtists).hasSize(1);
            assertThat(foundArtists).first().satisfies(artist -> {
                if (expectedArtist.getArtworkPath() != null) {
                    assertThat(artist.getArtwork()).satisfies(artwork ->
                            assertThat(artwork.getSourceUri())
                                    .isEqualTo(expectedArtworkPathToUri(expectedArtist.getArtworkPath(), context)));
                } else {
                    assertThat(artist.getArtwork()).isNull();
                }
                for (ScanTestPlan.ExpectedData.Artist.Album expectedAlbum : expectedArtist.getAlbums()) {
                    List<Album> foundAlbums = artist.getAlbums().stream()
                            .filter(album -> Objects.equals(album.getName(), expectedAlbum.getName()))
                            .toList();
                    assertThat(foundAlbums).hasSize(1);
                    assertThat(foundAlbums).first().satisfies(album -> {
                        assertThat(album.getYear()).isEqualTo(expectedAlbum.getYear());
                        if (expectedAlbum.getArtworkPath() != null) {
                            assertThat(album.getArtwork()).satisfies(artwork ->
                                    assertThat(artwork.getSourceUri())
                                            .isEqualTo(expectedArtworkPathToUri(expectedAlbum.getArtworkPath(), context)));
                        } else {
                            assertThat(album.getArtwork()).isNull();
                        }
                        List<Song> songs = album.getSongs();
                        assertThat(songs).hasSize(expectedAlbum.getSongPaths().size());
                        assertThat(songs.stream().map(Song::getPath))
                                .containsAll(expectedAlbum.getSongPaths().stream()
                                        .map(relativePath -> new File(context.getRootFolder().getAbsolutePath(), relativePath))
                                        .map(File::getAbsolutePath)
                                        .toList());
                    });
                }
            });
        }
    }

    private void verifySongs(Context context) {
        List<Song> songs = songRepository.findAll();
        assertThat(songs).hasSize(context.getScanTestPlan().getExpectedData().getSongs().size());
        for (ScanTestPlan.ExpectedData.Song expectedSong : context.getScanTestPlan().getExpectedData().getSongs()) {
            String expectedPath = new File(context.getRootFolder(), expectedSong.getPath()).getAbsolutePath();
            List<Song> foundSongs = songs.stream()
                    .filter(song -> Objects.equals(song.getPath(), expectedPath))
                    .toList();
            assertThat(foundSongs).hasSize(1);
            assertThat(foundSongs).first().satisfies(song -> {
                assertThat(song.getPath()).isEqualTo(expectedPath);
                assertThat(song.getFileType().getMimeType()).isEqualTo(expectedSong.getMimeType());
                assertThat(song.getFileType().getFileExtension()).isEqualTo(expectedSong.getFileExtension());
                assertThat(song.getSize()).isNotNull();
                assertThat(song.getDuration()).isNotNull();
                assertThat(song.getBitRate()).isNotNull();
                assertThat(song.getBitRateVariable()).isNotNull();
                assertThat(song.getDiscNumber()).isEqualTo(expectedSong.getDiscNumber());
                assertThat(song.getDiscCount()).isEqualTo(expectedSong.getDiscCount());
                assertThat(song.getTrackNumber()).isEqualTo(expectedSong.getTrackNumber());
                assertThat(song.getTrackCount()).isEqualTo(expectedSong.getTrackCount());
                assertThat(song.getName()).isEqualTo(expectedSong.getTitle());
                assertThat(song.getGenreName()).isEqualTo(expectedSong.getGenre());
                assertThat(song.getArtistName()).isEqualTo(expectedSong.getArtist());
                assertThat(song.getAlbumArtistName()).isEqualTo(expectedSong.getAlbumArtist());
                assertThat(song.getAlbumName()).isEqualTo(expectedSong.getAlbum());
                assertThat(song.getYear()).isEqualTo(expectedSong.getYear());
                if (expectedSong.getArtworkPath() != null) {
                    assertThat(song.getArtwork()).satisfies(artwork ->
                            assertThat(artwork.getSourceUri())
                                    .isEqualTo(expectedArtworkPathToUri(expectedSong.getArtworkPath(), context)));
                } else {
                    assertThat(song.getArtwork()).isNull();
                }
            });
        }
    }

    private URI expectedArtworkPathToUri(String artworkPath, Context context) {
        URI artworkUri = UriComponentsBuilder
                .fromUriString(artworkPath)
                .build()
                .toUri();
        return UriComponentsBuilder
                .fromPath(FilenameUtils.separatorsToUnix(new File(context.getRootFolder(), artworkUri.getPath()).getAbsolutePath()))
                .scheme(artworkUri.getScheme())
                .build()
                .toUri();
    }
}
