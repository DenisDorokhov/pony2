package net.dorokhov.pony2.api.library.domain;

import org.junit.jupiter.api.Test;

import static net.dorokhov.pony2.api.library.domain.ScanType.FULL;
import static org.assertj.core.api.Assertions.assertThat;

public class ScanResultTest {

    @Test
    public void shouldSupportEqualityAndHashCode() {

        ScanResult eqScanResult1 = scanResult(FULL).setId("1");
        ScanResult eqScanResult2 = scanResult(FULL).setId("1");
        ScanResult diffScanResult = scanResult(FULL).setId("2");

        assertThat(eqScanResult1.hashCode()).isEqualTo(eqScanResult2.hashCode());
        assertThat(eqScanResult1.hashCode()).isNotEqualTo(diffScanResult.hashCode());

        assertThat(eqScanResult1).isEqualTo(eqScanResult1);
        assertThat(eqScanResult1).isEqualTo(eqScanResult2);

        assertThat(eqScanResult1).isNotEqualTo(diffScanResult);
        assertThat(eqScanResult1).isNotEqualTo("foo1");
        assertThat(eqScanResult1).isNotEqualTo(null);
    }
    
    private ScanResult scanResult(ScanType scanType) {
        return new ScanResult()
                .setScanType(scanType)
                .setDuration(10L)
                .setSongSize(1000L)
                .setArtworkSize(5L)
                .setGenreCount(1)
                .setArtistCount(2)
                .setAlbumCount(3)
                .setSongCount(4)
                .setArtworkCount(5)
                .setProcessedAudioFileCount(32)
                .setCreatedArtistCount(7)
                .setUpdatedArtistCount(8)
                .setDeletedArtistCount(9)
                .setCreatedAlbumCount(10)
                .setUpdatedAlbumCount(11)
                .setDeletedAlbumCount(12)
                .setCreatedGenreCount(13)
                .setUpdatedGenreCount(14)
                .setDeletedGenreCount(15)
                .setCreatedSongCount(16)
                .setUpdatedSongCount(17)
                .setDeletedSongCount(18)
                .setCreatedArtworkCount(19)
                .setDeletedArtworkCount(20);
    }
}