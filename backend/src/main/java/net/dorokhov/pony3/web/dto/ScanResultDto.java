package net.dorokhov.pony3.web.dto;

import net.dorokhov.pony3.api.library.domain.ScanResult;
import net.dorokhov.pony3.api.library.domain.ScanType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ScanResultDto {

    private String id;
    private LocalDateTime date;

    private ScanType scanType;

    private List<String> failedPaths = new ArrayList<>();

    private Integer processedAudioFileCount;

    private Long duration;
    private Long songSize;
    private Long artworkSize;

    private Integer genreCount;
    private Integer artistCount;
    private Integer albumCount;
    private Integer songCount;
    private Integer artworkCount;

    private Integer createdArtistCount;
    private Integer updatedArtistCount;
    private Integer deletedArtistCount;

    private Integer createdAlbumCount;
    private Integer updatedAlbumCount;
    private Integer deletedAlbumCount;

    private Integer createdGenreCount;
    private Integer updatedGenreCount;
    private Integer deletedGenreCount;

    private Integer createdSongCount;
    private Integer updatedSongCount;
    private Integer deletedSongCount;

    private Integer createdArtworkCount;
    private Integer deletedArtworkCount;

    public String getId() {
        return id;
    }

    public ScanResultDto setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public ScanResultDto setDate(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public ScanResultDto setScanType(ScanType scanType) {
        this.scanType = scanType;
        return this;
    }

    public List<String> getFailedPaths() {
        if (failedPaths == null) {
            failedPaths = new ArrayList<>();
        }
        return failedPaths;
    }

    public ScanResultDto setFailedPaths(List<String> failedPaths) {
        this.failedPaths = failedPaths;
        return this;
    }

    public Integer getProcessedAudioFileCount() {
        return processedAudioFileCount;
    }

    public ScanResultDto setProcessedAudioFileCount(Integer processedAudioFileCount) {
        this.processedAudioFileCount = processedAudioFileCount;
        return this;
    }

    public Long getDuration() {
        return duration;
    }

    public ScanResultDto setDuration(Long duration) {
        this.duration = duration;
        return this;
    }

    public Long getSongSize() {
        return songSize;
    }

    public ScanResultDto setSongSize(Long songSize) {
        this.songSize = songSize;
        return this;
    }

    public Long getArtworkSize() {
        return artworkSize;
    }

    public ScanResultDto setArtworkSize(Long artworkSize) {
        this.artworkSize = artworkSize;
        return this;
    }

    public Integer getGenreCount() {
        return genreCount;
    }

    public ScanResultDto setGenreCount(Integer genreCount) {
        this.genreCount = genreCount;
        return this;
    }

    public Integer getArtistCount() {
        return artistCount;
    }

    public ScanResultDto setArtistCount(Integer artistCount) {
        this.artistCount = artistCount;
        return this;
    }

    public Integer getAlbumCount() {
        return albumCount;
    }

    public ScanResultDto setAlbumCount(Integer albumCount) {
        this.albumCount = albumCount;
        return this;
    }

    public Integer getSongCount() {
        return songCount;
    }

    public ScanResultDto setSongCount(Integer songCount) {
        this.songCount = songCount;
        return this;
    }

    public Integer getArtworkCount() {
        return artworkCount;
    }

    public ScanResultDto setArtworkCount(Integer artworkCount) {
        this.artworkCount = artworkCount;
        return this;
    }

    public Integer getCreatedArtistCount() {
        return createdArtistCount;
    }

    public ScanResultDto setCreatedArtistCount(Integer createdArtistCount) {
        this.createdArtistCount = createdArtistCount;
        return this;
    }

    public Integer getUpdatedArtistCount() {
        return updatedArtistCount;
    }

    public ScanResultDto setUpdatedArtistCount(Integer updatedArtistCount) {
        this.updatedArtistCount = updatedArtistCount;
        return this;
    }

    public Integer getDeletedArtistCount() {
        return deletedArtistCount;
    }

    public ScanResultDto setDeletedArtistCount(Integer deletedArtistCount) {
        this.deletedArtistCount = deletedArtistCount;
        return this;
    }

    public Integer getCreatedAlbumCount() {
        return createdAlbumCount;
    }

    public ScanResultDto setCreatedAlbumCount(Integer createdAlbumCount) {
        this.createdAlbumCount = createdAlbumCount;
        return this;
    }

    public Integer getUpdatedAlbumCount() {
        return updatedAlbumCount;
    }

    public ScanResultDto setUpdatedAlbumCount(Integer updatedAlbumCount) {
        this.updatedAlbumCount = updatedAlbumCount;
        return this;
    }

    public Integer getDeletedAlbumCount() {
        return deletedAlbumCount;
    }

    public ScanResultDto setDeletedAlbumCount(Integer deletedAlbumCount) {
        this.deletedAlbumCount = deletedAlbumCount;
        return this;
    }

    public Integer getCreatedGenreCount() {
        return createdGenreCount;
    }

    public ScanResultDto setCreatedGenreCount(Integer createdGenreCount) {
        this.createdGenreCount = createdGenreCount;
        return this;
    }

    public Integer getUpdatedGenreCount() {
        return updatedGenreCount;
    }

    public ScanResultDto setUpdatedGenreCount(Integer updatedGenreCount) {
        this.updatedGenreCount = updatedGenreCount;
        return this;
    }

    public Integer getDeletedGenreCount() {
        return deletedGenreCount;
    }

    public ScanResultDto setDeletedGenreCount(Integer deletedGenreCount) {
        this.deletedGenreCount = deletedGenreCount;
        return this;
    }

    public Integer getCreatedSongCount() {
        return createdSongCount;
    }

    public ScanResultDto setCreatedSongCount(Integer createdSongCount) {
        this.createdSongCount = createdSongCount;
        return this;
    }

    public Integer getUpdatedSongCount() {
        return updatedSongCount;
    }

    public ScanResultDto setUpdatedSongCount(Integer updatedSongCount) {
        this.updatedSongCount = updatedSongCount;
        return this;
    }

    public Integer getDeletedSongCount() {
        return deletedSongCount;
    }

    public ScanResultDto setDeletedSongCount(Integer deletedSongCount) {
        this.deletedSongCount = deletedSongCount;
        return this;
    }

    public Integer getCreatedArtworkCount() {
        return createdArtworkCount;
    }

    public ScanResultDto setCreatedArtworkCount(Integer createdArtworkCount) {
        this.createdArtworkCount = createdArtworkCount;
        return this;
    }

    public Integer getDeletedArtworkCount() {
        return deletedArtworkCount;
    }

    public ScanResultDto setDeletedArtworkCount(Integer deletedArtworkCount) {
        this.deletedArtworkCount = deletedArtworkCount;
        return this;
    }

    public static ScanResultDto of(ScanResult scanResult) {
        return new ScanResultDto()
                .setId(scanResult.getId())
                .setDate(scanResult.getDate())
                .setScanType(scanResult.getScanType())
                .setFailedPaths(scanResult.getFailedPaths())
                .setProcessedAudioFileCount(scanResult.getProcessedAudioFileCount())
                .setDuration(scanResult.getDuration())
                .setSongSize(scanResult.getSongSize())
                .setArtworkSize(scanResult.getArtworkSize())
                .setGenreCount(scanResult.getGenreCount())
                .setArtistCount(scanResult.getArtistCount())
                .setAlbumCount(scanResult.getAlbumCount())
                .setSongCount(scanResult.getSongCount())
                .setArtworkCount(scanResult.getArtworkCount())
                .setCreatedArtistCount(scanResult.getCreatedArtistCount())
                .setUpdatedArtistCount(scanResult.getUpdatedArtistCount())
                .setDeletedArtistCount(scanResult.getDeletedArtistCount())
                .setCreatedAlbumCount(scanResult.getCreatedAlbumCount())
                .setUpdatedAlbumCount(scanResult.getUpdatedAlbumCount())
                .setDeletedAlbumCount(scanResult.getDeletedAlbumCount())
                .setCreatedGenreCount(scanResult.getCreatedGenreCount())
                .setUpdatedGenreCount(scanResult.getUpdatedGenreCount())
                .setDeletedGenreCount(scanResult.getDeletedGenreCount())
                .setCreatedSongCount(scanResult.getCreatedSongCount())
                .setUpdatedSongCount(scanResult.getUpdatedSongCount())
                .setDeletedSongCount(scanResult.getDeletedSongCount())
                .setCreatedArtworkCount(scanResult.getCreatedArtworkCount())
                .setDeletedArtworkCount(scanResult.getDeletedArtworkCount());
    }
}
