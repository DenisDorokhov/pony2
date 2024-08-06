package net.dorokhov.pony2.test;

import net.dorokhov.pony2.api.library.domain.ScanResult;
import net.dorokhov.pony2.api.library.domain.ScanType;

public final class ScanResultFixtures {

    private ScanResultFixtures() {
    }

    public static ScanResult scanResult(ScanType scanType) {
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
