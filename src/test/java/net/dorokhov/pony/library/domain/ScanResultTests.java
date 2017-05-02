package net.dorokhov.pony.library.domain;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanResultTests {

    @Test
    public void stringify() throws Exception {
        assertThat(ScanResult.builder()
                .id(123L)
                .date(LocalDateTime.now())
                .scanType(ScanType.FULL)
                .targetPaths(ImmutableList.of())
                .failedPaths(ImmutableList.of())
                .duration(10L)
                .songSize(1000L)
                .artworkSize(5L)
                .genreCount(1L)
                .artistCount(2L)
                .albumCount(3L)
                .songCount(4L)
                .artworkCount(5L)
                .audioFileCount(32L)
                .imageFileCount(64L)
                .createdArtistCount(7L)
                .updatedArtistCount(8L)
                .deletedArtistCount(9L)
                .createdAlbumCount(10L)
                .updatedAlbumCount(11L)
                .deletedAlbumCount(12L)
                .createdGenreCount(13L)
                .updatedGenreCount(14L)
                .deletedGenreCount(15L)
                .createdSongCount(16L)
                .updatedSongCount(17L)
                .deletedSongCount(18L)
                .createdArtworkCount(19L)
                .deletedArtworkCount(20L)
                .build().toString()).startsWith("ScanResult{");
    }
}
