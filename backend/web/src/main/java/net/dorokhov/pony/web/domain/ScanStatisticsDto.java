package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.ScanResult;

import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ScanStatisticsDto {

    private final LocalDateTime date;
    private final Long duration;
    private final Long songSize;
    private final Long artworkSize;
    private final Integer genreCount;
    private final Integer artistCount;
    private final Integer albumCount;
    private final Integer songCount;
    private final Integer artworkCount;

    private ScanStatisticsDto(
            LocalDateTime date, 
            Long duration, Long songSize, Long artworkSize,
            Integer genreCount, Integer artistCount, Integer albumCount, Integer songCount, Integer artworkCount
    ) {
        this.date = checkNotNull(date);
        this.duration = checkNotNull(duration);
        this.songSize = checkNotNull(songSize);
        this.artworkSize = checkNotNull(artworkSize);
        this.genreCount = checkNotNull(genreCount);
        this.artistCount = checkNotNull(artistCount);
        this.albumCount = checkNotNull(albumCount);
        this.songCount = checkNotNull(songCount);
        this.artworkCount = checkNotNull(artworkCount);
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Long getDuration() {
        return duration;
    }

    public Long getSongSize() {
        return songSize;
    }

    public Long getArtworkSize() {
        return artworkSize;
    }

    public Integer getGenreCount() {
        return genreCount;
    }

    public Integer getArtistCount() {
        return artistCount;
    }

    public Integer getAlbumCount() {
        return albumCount;
    }

    public Integer getSongCount() {
        return songCount;
    }

    public Integer getArtworkCount() {
        return artworkCount;
    }

    public static ScanStatisticsDto of(ScanResult scanResult) {
        return new ScanStatisticsDto(
                scanResult.getDate(), 
                scanResult.getDuration(), scanResult.getSongSize(), scanResult.getArtworkSize(),
                scanResult.getGenreCount(), scanResult.getArtistCount(), scanResult.getAlbumCount(), scanResult.getSongCount(), scanResult.getArtworkCount()
        );
    }
}
