package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanType;
import net.dorokhov.pony.library.repository.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static net.dorokhov.pony.fixture.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony.fixture.ScanResultFixtures.scanResultBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
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
    public void shouldCalculateAndSaveFirstScan() throws Exception {
        given(scanResultRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of()));
        doTestCalculateAndSave(LocalDateTime.MIN);
    }

    @Test
    public void shouldCalculateAndSaveConsequentScan() throws Exception {
        LocalDateTime lastScanDate = LocalDateTime.now();
        given(scanResultRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(
                ImmutableList.of(scanResultBuilder(ScanType.FULL).date(lastScanDate).build())));
        doTestCalculateAndSave(lastScanDate);
    }
    
    private void doTestCalculateAndSave(LocalDateTime lastScanDate) {

        given(songRepository.count())
                .willReturn(0L)
                .willReturn(10L);
        given(songRepository.countByCreationDateGreaterThan(lastScanDate)).willReturn(10L);
        given(songRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(lastScanDate, lastScanDate)).willReturn(0L);

        given(genreRepository.count())
                .willReturn(0L)
                .willReturn(2L);
        given(genreRepository.countByCreationDateGreaterThan(lastScanDate)).willReturn(2L);
        given(genreRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(lastScanDate, lastScanDate)).willReturn(0L);

        given(artistRepository.count())
                .willReturn(0L)
                .willReturn(3L);
        given(artistRepository.countByCreationDateGreaterThan(lastScanDate)).willReturn(3L);
        given(artistRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(lastScanDate, lastScanDate)).willReturn(0L);

        given(albumRepository.count())
                .willReturn(0L)
                .willReturn(4L);
        given(albumRepository.countByCreationDateGreaterThan(lastScanDate)).willReturn(4L);
        given(albumRepository.countByCreationDateLessThanAndUpdateDateGreaterThan(lastScanDate, lastScanDate)).willReturn(0L);

        given(artworkRepository.count())
                .willReturn(0L)
                .willReturn(5L);
        given(artworkRepository.countByDateGreaterThan(lastScanDate)).willReturn(5L);

        given(songRepository.sumSize()).willReturn(123L);
        given(artworkRepository.sumLargeImageSize()).willReturn(100L);
        given(artworkRepository.sumSmallImageSize()).willReturn(200L);

        given(scanResultRepository.save((ScanResult) any())).willAnswer(returnsFirstArg());

        ScanResult scanResult = scanResultCalculator.calculateAndSave(rethrow(() -> {
            Thread.sleep(100);
            return new ScanResultCalculator.AudioFileProcessingResult() {
                @Override
                public ScanType getScanType() {
                    return ScanType.FULL;
                }

                @Override
                @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
                public List<File> getTargetFiles() {
                    return ImmutableList.of(new File("/file1"), new File("/file2"));
                }

                @Override
                @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
                public List<File> getFailedFiles() {
                    return ImmutableList.of(new File("/file3"));
                }

                @Override
                public int getProcessedAudioFileCount() {
                    return 20;
                }
            };
        }));

        verify(scanResultRepository).save((ScanResult) any());

        assertThat(scanResult.getScanType()).isEqualTo(ScanType.FULL);
        assertThat(scanResult.getTargetPaths()).containsExactly("/file1", "/file2");
        assertThat(scanResult.getFailedPaths()).containsExactly("/file3");
        assertThat(scanResult.getProcessedAudioFileCount()).isEqualTo(20);
        assertThat(scanResult.getDuration()).isGreaterThanOrEqualTo(100L);
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