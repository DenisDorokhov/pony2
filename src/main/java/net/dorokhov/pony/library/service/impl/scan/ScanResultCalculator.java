package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.base.Stopwatch;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanType;
import net.dorokhov.pony.library.repository.*;
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

    public ScanResultCalculator(ScanResultRepository scanResultRepository,
                                GenreRepository genreRepository,
                                ArtistRepository artistRepository,
                                AlbumRepository albumRepository,
                                SongRepository songRepository,
                                ArtworkRepository artworkRepository,
                                PlatformTransactionManager transactionManager) {
        
        this.scanResultRepository = scanResultRepository;
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
        this.artworkRepository = artworkRepository;
        
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public ScanResult calculateAndSave(Supplier<AudioFileProcessingResult> audioFileProcessor) {
        
        ScanStateBeforeProcessing scanStateBeforeProcessing = transactionTemplate.execute(status -> new ScanStateBeforeProcessing());

        Stopwatch stopwatch = Stopwatch.createStarted();
        AudioFileProcessingResult processingResult = audioFileProcessor.get();
        long duration = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        return transactionTemplate.execute(status -> doCalculateAndSave(processingResult, duration, scanStateBeforeProcessing));
    }
    
    private ScanResult doCalculateAndSave(AudioFileProcessingResult processingResult, long duration, ScanStateBeforeProcessing scanStateBeforeProcessing) {

        long songCountAfterScan = songRepository.count();
        long songCountCreated = songRepository.countByCreationDateGreaterThan(scanStateBeforeProcessing.getLastScanDate());
        long songCountUpdated = songRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(scanStateBeforeProcessing.getLastScanDate(), scanStateBeforeProcessing.getLastScanDate());
        long songCountDeleted = max(0, scanStateBeforeProcessing.getSongCountBeforeScan() - (songCountAfterScan - songCountCreated));

        long genreCountAfterScan = genreRepository.count();
        long genreCountCreated = genreRepository.countByCreationDateGreaterThan(scanStateBeforeProcessing.getLastScanDate());
        long genreCountUpdated = genreRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(scanStateBeforeProcessing.getLastScanDate(), scanStateBeforeProcessing.getLastScanDate());
        long genreCountDeleted = max(0, scanStateBeforeProcessing.getGenreCountBeforeScan() - (genreCountAfterScan - genreCountCreated));

        long artistCountAfterScan = artistRepository.count();
        long artistCountCreated = artistRepository.countByCreationDateGreaterThan(scanStateBeforeProcessing.getLastScanDate());
        long artistCountUpdated = artistRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(scanStateBeforeProcessing.getLastScanDate(), scanStateBeforeProcessing.getLastScanDate());
        long artistCountDeleted = max(0, scanStateBeforeProcessing.getArtistCountBeforeScan() - (artistCountAfterScan - artistCountCreated));

        long albumCountAfterScan = albumRepository.count();
        long albumCountCreated = albumRepository.countByCreationDateGreaterThan(scanStateBeforeProcessing.getLastScanDate());
        long albumCountUpdated = albumRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(scanStateBeforeProcessing.getLastScanDate(), scanStateBeforeProcessing.getLastScanDate());
        long albumCountDeleted = max(0, scanStateBeforeProcessing.getAlbumCountBeforeScan() - (albumCountAfterScan - albumCountCreated));

        long artworkCountAfterScan = artworkRepository.count();
        long artworkCountCreated = artworkRepository.countByDateGreaterThan(scanStateBeforeProcessing.getLastScanDate());
        long artworkCountDeleted = max(0, scanStateBeforeProcessing.getArtworkCountBeforeScan() - (artworkCountAfterScan - artworkCountCreated));

        ScanResult.Builder builder = ScanResult.builder();

        builder.scanType(processingResult.getScanType());
        builder.targetPaths(filesToPaths(processingResult.getTargetFiles()));
        builder.failedPaths(filesToPaths(processingResult.getFailedFiles()));
        builder.processedAudioFileCount(processingResult.getProcessedAudioFileCount());
        builder.duration(duration);

        builder.songSize(songRepository.sumSize());
        builder.artworkSize(artworkRepository.sumLargeImageSize() + artworkRepository.sumSmallImageSize());

        builder.genreCount(toIntExact(genreCountAfterScan));
        builder.artistCount(toIntExact(artistCountAfterScan));
        builder.albumCount(toIntExact(albumCountAfterScan));
        builder.songCount(toIntExact(songCountAfterScan));
        builder.artworkCount(toIntExact(artworkCountAfterScan));

        builder.createdArtistCount(toIntExact(artistCountCreated));
        builder.updatedArtistCount(toIntExact(artistCountUpdated));
        builder.deletedArtistCount(toIntExact(artistCountDeleted));

        builder.createdAlbumCount(toIntExact(albumCountCreated));
        builder.updatedAlbumCount(toIntExact(albumCountUpdated));
        builder.deletedAlbumCount(toIntExact(albumCountDeleted));

        builder.createdGenreCount(toIntExact(genreCountCreated));
        builder.updatedGenreCount(toIntExact(genreCountUpdated));
        builder.deletedGenreCount(toIntExact(genreCountDeleted));

        builder.createdSongCount(toIntExact(songCountCreated));
        builder.updatedSongCount(toIntExact(songCountUpdated));
        builder.deletedSongCount(toIntExact(songCountDeleted));

        builder.createdArtworkCount(toIntExact(artworkCountCreated));
        builder.deletedArtworkCount(toIntExact(artworkCountDeleted));

        return scanResultRepository.save(builder.build());
    }

    private List<String> filesToPaths(List<File> files) {
        return files.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
    }
    
    private class ScanStateBeforeProcessing {

        private final LocalDateTime lastScanDate;
        private final long songCountBeforeScan;
        private final long genreCountBeforeScan;
        private final long artistCountBeforeScan;
        private final long albumCountBeforeScan;
        private final long artworkCountBeforeScan;

        public ScanStateBeforeProcessing() {
            
            Page<ScanResult> scanResults = scanResultRepository.findAll(new PageRequest(0, 1, Sort.Direction.DESC, "date"));
            lastScanDate = scanResults.getContent().stream()
                    .findFirst()
                    .map(ScanResult::getDate)
                    .orElse(LocalDateTime.MIN);

            songCountBeforeScan = songRepository.count();
            genreCountBeforeScan = genreRepository.count();
            artistCountBeforeScan = artistRepository.count();
            albumCountBeforeScan = albumRepository.count();
            artworkCountBeforeScan = artworkRepository.count();
        }

        public LocalDateTime getLastScanDate() {
            return lastScanDate;
        }

        public long getSongCountBeforeScan() {
            return songCountBeforeScan;
        }

        public long getGenreCountBeforeScan() {
            return genreCountBeforeScan;
        }

        public long getArtistCountBeforeScan() {
            return artistCountBeforeScan;
        }

        public long getAlbumCountBeforeScan() {
            return albumCountBeforeScan;
        }

        public long getArtworkCountBeforeScan() {
            return artworkCountBeforeScan;
        }
    }
}
