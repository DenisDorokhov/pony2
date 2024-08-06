package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.ScanResult;

import java.time.LocalDateTime;

public final class ScanStatisticsDto {

    private LocalDateTime date;
    private Long duration;
    private Long songSize;
    private Long artworkSize;
    private Integer genreCount;
    private Integer artistCount;
    private Integer albumCount;
    private Integer songCount;
    private Integer artworkCount;

    public LocalDateTime getDate() {
        return date;
    }

    public ScanStatisticsDto setDate(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public Long getDuration() {
        return duration;
    }

    public ScanStatisticsDto setDuration(Long duration) {
        this.duration = duration;
        return this;
    }

    public Long getSongSize() {
        return songSize;
    }

    public ScanStatisticsDto setSongSize(Long songSize) {
        this.songSize = songSize;
        return this;
    }

    public Long getArtworkSize() {
        return artworkSize;
    }

    public ScanStatisticsDto setArtworkSize(Long artworkSize) {
        this.artworkSize = artworkSize;
        return this;
    }

    public Integer getGenreCount() {
        return genreCount;
    }

    public ScanStatisticsDto setGenreCount(Integer genreCount) {
        this.genreCount = genreCount;
        return this;
    }

    public Integer getArtistCount() {
        return artistCount;
    }

    public ScanStatisticsDto setArtistCount(Integer artistCount) {
        this.artistCount = artistCount;
        return this;
    }

    public Integer getAlbumCount() {
        return albumCount;
    }

    public ScanStatisticsDto setAlbumCount(Integer albumCount) {
        this.albumCount = albumCount;
        return this;
    }

    public Integer getSongCount() {
        return songCount;
    }

    public ScanStatisticsDto setSongCount(Integer songCount) {
        this.songCount = songCount;
        return this;
    }

    public Integer getArtworkCount() {
        return artworkCount;
    }

    public ScanStatisticsDto setArtworkCount(Integer artworkCount) {
        this.artworkCount = artworkCount;
        return this;
    }

    public static ScanStatisticsDto of(ScanResult scanResult) {
        return new ScanStatisticsDto()
                .setDate(scanResult.getDate())
                .setDuration(scanResult.getDuration())
                .setSongSize(scanResult.getSongSize())
                .setArtworkSize(scanResult.getArtworkSize())
                .setGenreCount(scanResult.getGenreCount())
                .setArtistCount(scanResult.getArtistCount())
                .setAlbumCount(scanResult.getAlbumCount())
                .setSongCount(scanResult.getSongCount())
                .setArtworkCount(scanResult.getArtworkCount());
    }
}
