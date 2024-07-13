package net.dorokhov.pony3.core.library.service.scan;

import com.google.common.base.Stopwatch;
import net.dorokhov.pony3.api.library.domain.ScanResult;
import net.dorokhov.pony3.api.library.domain.ScanType;
import net.dorokhov.pony3.core.library.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.toIntExact;
import static java.util.Objects.requireNonNull;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Component
public class ScanResultCalculator {

    public interface AudioFileProcessingResult {
        ScanType getScanType();

        List<File> getTargetFiles();

        List<File> getFailedFiles();

        int getProcessedAudioFileCount();
    }

    private final ScanResultRepository scanResultRepository;
    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final ArtworkRepository artworkRepository;

    private final TransactionTemplate transactionTemplate;

    public ScanResultCalculator(
            ScanResultRepository scanResultRepository,
            GenreRepository genreRepository,
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            SongRepository songRepository,
            ArtworkRepository artworkRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.scanResultRepository = scanResultRepository;
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
        this.artworkRepository = artworkRepository;

        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public ScanResult calculateAndSave(Supplier<AudioFileProcessingResult> audioFileProcessor) {

        ScanStateBeforeProcessing scanStateBeforeProcessing = requireNonNull(transactionTemplate.execute(_ -> new ScanStateBeforeProcessing()));

        Stopwatch stopwatch = Stopwatch.createStarted();
        AudioFileProcessingResult processingResult = audioFileProcessor.get();
        long duration = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        return transactionTemplate.execute(_ -> doCalculateAndSave(processingResult, duration, scanStateBeforeProcessing));
    }

    private ScanResult doCalculateAndSave(AudioFileProcessingResult processingResult, long duration, ScanStateBeforeProcessing scanStateBeforeProcessing) {

        long songCountAfterScan = songRepository.count();
        long songCountCreated = songRepository.countByCreationDateGreaterThan(scanStateBeforeProcessing.lastScanDate);
        long songCountUpdated = songRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(scanStateBeforeProcessing.lastScanDate, scanStateBeforeProcessing.lastScanDate);
        long songCountDeleted = max(0, scanStateBeforeProcessing.songCountBeforeScan - (songCountAfterScan - songCountCreated));

        long genreCountAfterScan = genreRepository.count();
        long genreCountCreated = genreRepository.countByCreationDateGreaterThan(scanStateBeforeProcessing.lastScanDate);
        long genreCountUpdated = genreRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(scanStateBeforeProcessing.lastScanDate, scanStateBeforeProcessing.lastScanDate);
        long genreCountDeleted = max(0, scanStateBeforeProcessing.genreCountBeforeScan - (genreCountAfterScan - genreCountCreated));

        long artistCountAfterScan = artistRepository.count();
        long artistCountCreated = artistRepository.countByCreationDateGreaterThan(scanStateBeforeProcessing.lastScanDate);
        long artistCountUpdated = artistRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(scanStateBeforeProcessing.lastScanDate, scanStateBeforeProcessing.lastScanDate);
        long artistCountDeleted = max(0, scanStateBeforeProcessing.artistCountBeforeScan - (artistCountAfterScan - artistCountCreated));

        long albumCountAfterScan = albumRepository.count();
        long albumCountCreated = albumRepository.countByCreationDateGreaterThan(scanStateBeforeProcessing.lastScanDate);
        long albumCountUpdated = albumRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(scanStateBeforeProcessing.lastScanDate, scanStateBeforeProcessing.lastScanDate);
        long albumCountDeleted = max(0, scanStateBeforeProcessing.albumCountBeforeScan - (albumCountAfterScan - albumCountCreated));

        long artworkCountAfterScan = artworkRepository.count();
        long artworkCountCreated = artworkRepository.countByDateGreaterThan(scanStateBeforeProcessing.lastScanDate);
        long artworkCountDeleted = max(0, scanStateBeforeProcessing.artworkCountBeforeScan - (artworkCountAfterScan - artworkCountCreated));

        ScanResult scanResult = new ScanResult()

                .setScanType(processingResult.getScanType())
                .setTargetPaths(filesToPaths(processingResult.getTargetFiles()))
                .setFailedPaths(filesToPaths(processingResult.getFailedFiles()))
                .setProcessedAudioFileCount(processingResult.getProcessedAudioFileCount())
                .setDuration(duration)

                .setSongSize(songRepository.sumSize())
                .setArtworkSize(artworkRepository.sumLargeImageSize() + artworkRepository.sumSmallImageSize())

                .setGenreCount(toIntExact(genreCountAfterScan))
                .setArtistCount(toIntExact(artistCountAfterScan))
                .setAlbumCount(toIntExact(albumCountAfterScan))
                .setSongCount(toIntExact(songCountAfterScan))
                .setArtworkCount(toIntExact(artworkCountAfterScan))

                .setCreatedArtistCount(toIntExact(artistCountCreated))
                .setUpdatedArtistCount(toIntExact(artistCountUpdated))
                .setDeletedArtistCount(toIntExact(artistCountDeleted))

                .setCreatedAlbumCount(toIntExact(albumCountCreated))
                .setUpdatedAlbumCount(toIntExact(albumCountUpdated))
                .setDeletedAlbumCount(toIntExact(albumCountDeleted))

                .setCreatedGenreCount(toIntExact(genreCountCreated))
                .setUpdatedGenreCount(toIntExact(genreCountUpdated))
                .setDeletedGenreCount(toIntExact(genreCountDeleted))

                .setCreatedSongCount(toIntExact(songCountCreated))
                .setUpdatedSongCount(toIntExact(songCountUpdated))
                .setDeletedSongCount(toIntExact(songCountDeleted))

                .setCreatedArtworkCount(toIntExact(artworkCountCreated))
                .setDeletedArtworkCount(toIntExact(artworkCountDeleted));

        return scanResultRepository.save(scanResult);
    }

    private List<String> filesToPaths(List<File> files) {
        return files.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
    }

    private class ScanStateBeforeProcessing {

        public final LocalDateTime lastScanDate;
        public final long songCountBeforeScan;
        public final long genreCountBeforeScan;
        public final long artistCountBeforeScan;
        public final long albumCountBeforeScan;
        public final long artworkCountBeforeScan;

        public ScanStateBeforeProcessing() {

            Page<ScanResult> scanResults = scanResultRepository.findAll(PageRequest.of(0, 1, Sort.Direction.DESC, "date"));
            lastScanDate = scanResults.getContent().stream()
                    .findFirst()
                    .map(ScanResult::getDate)
                    .orElse(LocalDateTime.now().minusSeconds(1));

            songCountBeforeScan = songRepository.count();
            genreCountBeforeScan = genreRepository.count();
            artistCountBeforeScan = artistRepository.count();
            albumCountBeforeScan = albumRepository.count();
            artworkCountBeforeScan = artworkRepository.count();
        }
    }
}
