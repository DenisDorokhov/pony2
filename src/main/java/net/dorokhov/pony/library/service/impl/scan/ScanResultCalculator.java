package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.base.Stopwatch;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanType;
import net.dorokhov.pony.library.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.toIntExact;

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

    public ScanResultCalculator(ScanResultRepository scanResultRepository,
                                GenreRepository genreRepository,
                                ArtistRepository artistRepository,
                                AlbumRepository albumRepository,
                                SongRepository songRepository, 
                                ArtworkRepository artworkRepository) {
        this.scanResultRepository = scanResultRepository;
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
        this.artworkRepository = artworkRepository;
    }

    @Transactional(readOnly = true)
    public ScanResult calculateAndSave(Supplier<AudioFileProcessingResult> audioFileProcessor) {

        Page<ScanResult> scanResults = scanResultRepository.findAll(new PageRequest(0, 1, Sort.Direction.DESC, "date"));
        LocalDateTime lastScanDate = scanResults.getContent().stream()
                .findFirst()
                .map(ScanResult::getDate)
                .orElse(LocalDateTime.MIN);

        long songCountBeforeScan = songRepository.count();
        long genreCountBeforeScan = genreRepository.count();
        long artistCountBeforeScan = artistRepository.count();
        long albumCountBeforeScan = albumRepository.count();
        long artworkCountBeforeScan = artworkRepository.count();

        Stopwatch stopwatch = Stopwatch.createStarted();
        AudioFileProcessingResult processingResult = audioFileProcessor.get();
        long duration = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        long songCountAfterScan = songRepository.count();
        long songCountCreated = songRepository.countByCreationDateGreaterThan(lastScanDate);
        long songCountUpdated = songRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(lastScanDate, lastScanDate);
        long songCountDeleted = max(0, songCountBeforeScan - (songCountAfterScan - songCountCreated));

        long genreCountAfterScan = genreRepository.count();
        long genreCountCreated = genreRepository.countByCreationDateGreaterThan(lastScanDate);
        long genreCountUpdated = genreRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(lastScanDate, lastScanDate);
        long genreCountDeleted = max(0, genreCountBeforeScan - (genreCountAfterScan - genreCountCreated));

        long artistCountAfterScan = artistRepository.count();
        long artistCountCreated = artistRepository.countByCreationDateGreaterThan(lastScanDate);
        long artistCountUpdated = artistRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(lastScanDate, lastScanDate);
        long artistCountDeleted = max(0, artistCountBeforeScan - (artistCountAfterScan - artistCountCreated));

        long albumCountAfterScan = albumRepository.count();
        long albumCountCreated = albumRepository.countByCreationDateGreaterThan(lastScanDate);
        long albumCountUpdated = albumRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(lastScanDate, lastScanDate);
        long albumCountDeleted = max(0, albumCountBeforeScan - (albumCountAfterScan - albumCountCreated));

        long artworkCountAfterScan = artworkRepository.count();
        long artworkCountCreated = artworkRepository.countByDateGreaterThan(lastScanDate);
        long artworkCountDeleted = max(0, artworkCountBeforeScan - (artworkCountAfterScan - artworkCountCreated));

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
}
