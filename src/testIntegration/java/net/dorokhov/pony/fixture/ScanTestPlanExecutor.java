package net.dorokhov.pony.fixture;

import com.google.common.io.Files;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.domain.ScanType;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.library.service.ScanJobService;
import net.dorokhov.pony.library.service.impl.audio.AudioTagger;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.log.domain.LogMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private static final Resource AUDIO_TEMPLATE = new ClassPathResource("audio/empty.mp3");

    private final AudioTagger audioTagger;
    private final ScanJobService scanJobService;
    private final LibraryService libraryService;

    public ScanTestPlanExecutor(AudioTagger audioTagger, ScanJobService scanJobService, LibraryService libraryService) {
        this.audioTagger = audioTagger;
        this.scanJobService = scanJobService;
        this.libraryService = libraryService;
    }

    public Context prepare(ScanTestPlan scanTestPlan) throws IOException {
        File rootFolder = Files.createTempDir();
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
            new File(rootFolder, filePath).delete();
        }
        return new Context(scanTestPlan, rootFolder);
    }

    @Transactional(readOnly = true)
    public void verify(Long scanJobId, Context context) {
        verifyScanJob(scanJobService.getById(scanJobId), context);
        verifyGenres(context);
        verifyArtists(context);
        verifySongs(context);
    }

    private void writeAudioData(File file, ScanTestPlan.SongToGenerate songToGenerate) throws IOException {
        audioTagger.write(file, WritableAudioData.builder()
                .discNumber(songToGenerate.getDiscNumber())
                .discCount(songToGenerate.getDiscCount())
                .trackNumber(songToGenerate.getTrackNumber())
                .trackCount(songToGenerate.getTrackCount())
                .title(songToGenerate.getTitle())
                .artist(songToGenerate.getArtist())
                .albumArtist(songToGenerate.getAlbumArtist())
                .album(songToGenerate.getAlbum())
                .year(songToGenerate.getYear())
                .genre(songToGenerate.getGenre())
                .artworkFile(Optional.ofNullable(songToGenerate.getEmbeddedArtwork())
                        .map(ScanTestPlan.EmbeddedArtwork::getCopyFrom)
                        .map(path -> {
                            try {
                                return new ClassPathResource(path).getFile();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .orElse(null))
                .build());
    }

    private void verifyScanJob(ScanJob scanJob, Context context) {
        assertThat(scanJob).isNotNull();
        assertThat(scanJob.getId()).isNotNull();
        assertThat(scanJob.getCreationDate()).isNotNull();
        assertThat(scanJob.getUpdateDate()).isNotNull();
        assertThat(scanJob.getScanType()).isSameAs(ScanType.FULL);
        assertThat(scanJob.getStatus()).isSameAs(ScanJob.Status.COMPLETE);
        assertThat(scanJob.getLogMessage()).satisfies(logMessage ->
                assertThat(logMessage.getLevel()).isSameAs(LogMessage.Level.INFO));
        assertThat(scanJob.getScanResult()).satisfies(scanResult -> {
            assertThat(scanResult.getId()).isNotNull();
            assertThat(scanResult.getDate()).isNotNull();
            assertThat(scanResult.getScanType()).isSameAs(ScanType.FULL);
            assertThat(scanResult.getTargetPaths()).containsExactly(context.getRootFolder().getAbsolutePath());
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
        List<Genre> genres = libraryService.getGenres();
        assertThat(genres).hasSize(context.getScanTestPlan().getExpectedData().getGenres().size());
        for (ScanTestPlan.ExpectedData.Genre expectedGenre : context.getScanTestPlan().getExpectedData().getGenres()) {
            List<Genre> foundGenres = genres.stream()
                    .filter(genre -> Objects.equals(genre.getName(), expectedGenre.getName()))
                    .collect(Collectors.toList());
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
                                .collect(Collectors.toList()));
            });
        }
    }

    private void verifyArtists(Context context) {
        // TODO: implement
    }

    private void verifySongs(Context context) {
        // TODO: implement
    }

    private URI expectedArtworkPathToUri(String artworkPath, Context context) {
        String[] parts = artworkPath.split(":");
        assertThat(parts).hasSize(2);
        return UriComponentsBuilder
                .fromPath(new File(context.getRootFolder(), parts[1]).getAbsolutePath())
                .scheme(parts[0])
                .build().toUri();
    }
}
