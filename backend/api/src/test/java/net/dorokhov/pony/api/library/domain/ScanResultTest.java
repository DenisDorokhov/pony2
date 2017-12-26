package net.dorokhov.pony.api.library.domain;

import org.junit.Test;

import static net.dorokhov.pony.api.library.domain.ScanType.FULL;
import static org.assertj.core.api.Assertions.assertThat;

public class ScanResultTest {

    @Test
    public void shouldSupportEqualityAndHashCode() {

        ScanResult eqScanResult1 = scanResultBuilder(FULL).id(1L).build();
        ScanResult eqScanResult2 = scanResultBuilder(FULL).id(1L).build();
        ScanResult diffScanResult = scanResultBuilder(FULL).id(2L).build();

        assertThat(eqScanResult1.hashCode()).isEqualTo(eqScanResult2.hashCode());
        assertThat(eqScanResult1.hashCode()).isNotEqualTo(diffScanResult.hashCode());

        assertThat(eqScanResult1).isEqualTo(eqScanResult1);
        assertThat(eqScanResult1).isEqualTo(eqScanResult2);

        assertThat(eqScanResult1).isNotEqualTo(diffScanResult);
        assertThat(eqScanResult1).isNotEqualTo("foo1");
        assertThat(eqScanResult1).isNotEqualTo(null);
    }
    
    private ScanResult.Builder scanResultBuilder(ScanType scanType) {
        return ScanResult.builder()
                .scanType(scanType)
                .duration(10L)
                .songSize(1000L)
                .artworkSize(5L)
                .genreCount(1)
                .artistCount(2)
                .albumCount(3)
                .songCount(4)
                .artworkCount(5)
                .processedAudioFileCount(32)
                .createdArtistCount(7)
                .updatedArtistCount(8)
                .deletedArtistCount(9)
                .createdAlbumCount(10)
                .updatedAlbumCount(11)
                .deletedAlbumCount(12)
                .createdGenreCount(13)
                .updatedGenreCount(14)
                .deletedGenreCount(15)
                .createdSongCount(16)
                .updatedSongCount(17)
                .deletedSongCount(18)
                .createdArtworkCount(19)
                .deletedArtworkCount(20);
    }
}