package net.dorokhov.pony.fixture;

import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanType;

public final class ScanResultFixtures {

    private ScanResultFixtures() {
    }
    
    public static ScanResult scanResult(ScanType scanType) {
        return scanResultBuilder(scanType).build();
    }
    
    public static ScanResult.Builder scanResultBuilder(ScanType scanType) {
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
