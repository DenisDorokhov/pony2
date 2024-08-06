package net.dorokhov.pony2.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.dorokhov.pony2.api.library.domain.ScanResult;
import net.dorokhov.pony2.api.library.domain.ScanType;
import net.dorokhov.pony2.core.library.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.annotation.Nullable;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony2.common.RethrowingLambdas.rethrow;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony2.test.ScanResultFixtures.scanResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ScanResultCalculatorTest {

    @InjectMocks
    private ScanResultCalculator scanResultCalculator;

    @Mock
    private ScanResultRepository scanResultRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private SongRepository songRepository;
    @Mock
    private ArtworkRepository artworkRepository;
    
    @Spy
    @SuppressWarnings("unused")
    private PlatformTransactionManager transactionManager = transactionManager();

    @Test
    public void shouldCalculateAndSaveFirstScan() {
        when(scanResultRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(emptyList()));
        doTestCalculateAndSave(null);
    }

    @Test
    public void shouldCalculateAndSaveConsequentScan() {
        LocalDateTime lastScanDate = LocalDateTime.now();
        when(scanResultRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(
                ImmutableList.of(scanResult(ScanType.FULL).setDate(lastScanDate))));
        doTestCalculateAndSave(lastScanDate);
    }
    
    private void doTestCalculateAndSave(@Nullable LocalDateTime lastScanDate) {

        when(songRepository.count())
                .thenReturn(0L)
                .thenReturn(10L);
        when(songRepository.countByCreationDateGreaterThan(lastScanDate != null ? lastScanDate : any()))
                .thenReturn(10L);
        when(songRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(
                lastScanDate != null ? lastScanDate : any(), 
                lastScanDate != null ? lastScanDate : any())).thenReturn(0L);

        when(genreRepository.count())
                .thenReturn(0L)
                .thenReturn(2L);
        when(genreRepository.countByCreationDateGreaterThan(lastScanDate != null ? lastScanDate : any()))
                .thenReturn(2L);
        when(genreRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(
                lastScanDate != null ? lastScanDate : any(), 
                lastScanDate != null ? lastScanDate : any())).thenReturn(0L);

        when(artistRepository.count())
                .thenReturn(0L)
                .thenReturn(3L);
        when(artistRepository.countByCreationDateGreaterThan(lastScanDate != null ? lastScanDate : any()))
                .thenReturn(3L);
        when(artistRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(
                lastScanDate != null ? lastScanDate : any(),
                lastScanDate != null ? lastScanDate : any())).thenReturn(0L);

        when(albumRepository.count())
                .thenReturn(0L)
                .thenReturn(4L);
        when(albumRepository.countByCreationDateGreaterThan(lastScanDate != null ? lastScanDate : any()))
                .thenReturn(4L);
        when(albumRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(
                lastScanDate != null ? lastScanDate : any(),
                lastScanDate != null ? lastScanDate : any())).thenReturn(0L);

        when(artworkRepository.count())
                .thenReturn(0L)
                .thenReturn(5L);
        when(artworkRepository.countByDateGreaterThan(lastScanDate != null ? lastScanDate : any()))
                .thenReturn(5L);

        when(songRepository.sumSize()).thenReturn(123L);
        when(songRepository.sumDuration()).thenReturn(234L);
        when(artworkRepository.sumLargeImageSize()).thenReturn(100L);
        when(artworkRepository.sumSmallImageSize()).thenReturn(200L);

        when(scanResultRepository.save(any())).then(returnsFirstArg());

        File failedFile = new File("/failedFile");
        ScanResult scanResult = scanResultCalculator.calculateAndSave(rethrow(() -> {
            Thread.sleep(150);
            return new ScanResultCalculator.AudioFileProcessingResult() {
                @Override
                public ScanType getScanType() {
                    return ScanType.FULL;
                }

                @Override
                @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
                public List<File> getFailedFiles() {
                    return ImmutableList.of(failedFile);
                }

                @Override
                public int getProcessedAudioFileCount() {
                    return 20;
                }
            };
        }));

        verify(scanResultRepository).save(any());

        assertThat(scanResult.getScanType()).isSameAs(ScanType.FULL);
        assertThat(scanResult.getFailedPaths()).containsExactly(failedFile.getAbsolutePath());
        assertThat(scanResult.getProcessedAudioFileCount()).isEqualTo(20);
        assertThat(scanResult.getDuration()).isEqualTo(234L);
        assertThat(scanResult.getSongSize()).isEqualTo(123L);
        assertThat(scanResult.getArtworkSize()).isEqualTo(300L);
        assertThat(scanResult.getGenreCount()).isEqualTo(2);
        assertThat(scanResult.getArtistCount()).isEqualTo(3);
        assertThat(scanResult.getAlbumCount()).isEqualTo(4);
        assertThat(scanResult.getSongCount()).isEqualTo(10);
        assertThat(scanResult.getArtworkCount()).isEqualTo(5);
        assertThat(scanResult.getCreatedArtistCount()).isEqualTo(3);
        assertThat(scanResult.getUpdatedArtistCount()).isEqualTo(0);
        assertThat(scanResult.getDeletedArtistCount()).isEqualTo(0);
        assertThat(scanResult.getCreatedAlbumCount()).isEqualTo(4);
        assertThat(scanResult.getUpdatedAlbumCount()).isEqualTo(0);
        assertThat(scanResult.getDeletedAlbumCount()).isEqualTo(0);
        assertThat(scanResult.getCreatedGenreCount()).isEqualTo(2);
        assertThat(scanResult.getUpdatedGenreCount()).isEqualTo(0);
        assertThat(scanResult.getDeletedGenreCount()).isEqualTo(0);
        assertThat(scanResult.getCreatedSongCount()).isEqualTo(10);
        assertThat(scanResult.getUpdatedSongCount()).isEqualTo(0);
        assertThat(scanResult.getDeletedSongCount()).isEqualTo(0);
        assertThat(scanResult.getCreatedArtworkCount()).isEqualTo(5);
        assertThat(scanResult.getDeletedArtworkCount()).isEqualTo(0);
    }
}