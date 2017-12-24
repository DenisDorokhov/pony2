package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.ScanResult;
import net.dorokhov.pony.api.library.domain.ScanType;

import java.time.LocalDateTime;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

public final class ScanResultDto {

    private final Long id;
    private final LocalDateTime date;
    
    private final ScanType scanType;
    
    private final List<String> targetPaths;
    private final List<String> failedPaths;
    
    private final Integer processedAudioFileCount;
    
    private final Long duration;
    private final Long songSize;
    private final Long artworkSize;
    
    private final Integer genreCount;
    private final Integer artistCount;
    private final Integer albumCount;
    private final Integer songCount;
    private final Integer artworkCount;
    
    private final Integer createdArtistCount;
    private final Integer updatedArtistCount;
    private final Integer deletedArtistCount;
    
    private final Integer createdAlbumCount;
    private final Integer updatedAlbumCount;
    private final Integer deletedAlbumCount;
    
    private final Integer createdGenreCount;
    private final Integer updatedGenreCount;
    private final Integer deletedGenreCount;
    
    private final Integer createdSongCount;
    private final Integer updatedSongCount;
    private final Integer deletedSongCount;
    
    private final Integer createdArtworkCount;
    private final Integer deletedArtworkCount;

    ScanResultDto(Long id, LocalDateTime date, ScanType scanType,
                  List<String> targetPaths, List<String> failedPaths,
                  Integer processedAudioFileCount, Long duration, Long songSize, Long artworkSize,
                  Integer genreCount, Integer artistCount, Integer albumCount, Integer songCount, Integer artworkCount,
                  Integer createdArtistCount, Integer updatedArtistCount, Integer deletedArtistCount,
                  Integer createdAlbumCount, Integer updatedAlbumCount, Integer deletedAlbumCount,
                  Integer createdGenreCount, Integer updatedGenreCount, Integer deletedGenreCount,
                  Integer createdSongCount, Integer updatedSongCount, Integer deletedSongCount,
                  Integer createdArtworkCount, Integer deletedArtworkCount) {
        this.id = checkNotNull(id);
        this.date = checkNotNull(date);
        this.scanType = checkNotNull(scanType);
        this.targetPaths = unmodifiableList(targetPaths);
        this.failedPaths = unmodifiableList(failedPaths);
        this.processedAudioFileCount = checkNotNull(processedAudioFileCount);
        this.duration = checkNotNull(duration);
        this.songSize = checkNotNull(songSize);
        this.artworkSize = checkNotNull(artworkSize);
        this.genreCount = checkNotNull(genreCount);
        this.artistCount = checkNotNull(artistCount);
        this.albumCount = checkNotNull(albumCount);
        this.songCount = checkNotNull(songCount);
        this.artworkCount = checkNotNull(artworkCount);
        this.createdArtistCount = checkNotNull(createdArtistCount);
        this.updatedArtistCount = checkNotNull(updatedArtistCount);
        this.deletedArtistCount = checkNotNull(deletedArtistCount);
        this.createdAlbumCount = checkNotNull(createdAlbumCount);
        this.updatedAlbumCount = checkNotNull(updatedAlbumCount);
        this.deletedAlbumCount = checkNotNull(deletedAlbumCount);
        this.createdGenreCount = checkNotNull(createdGenreCount);
        this.updatedGenreCount = checkNotNull(updatedGenreCount);
        this.deletedGenreCount = checkNotNull(deletedGenreCount);
        this.createdSongCount = checkNotNull(createdSongCount);
        this.updatedSongCount = checkNotNull(updatedSongCount);
        this.deletedSongCount = checkNotNull(deletedSongCount);
        this.createdArtworkCount = checkNotNull(createdArtworkCount);
        this.deletedArtworkCount = checkNotNull(deletedArtworkCount);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public List<String> getTargetPaths() {
        return targetPaths;
    }

    public List<String> getFailedPaths() {
        return failedPaths;
    }

    public Integer getProcessedAudioFileCount() {
        return processedAudioFileCount;
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

    public Integer getCreatedArtistCount() {
        return createdArtistCount;
    }

    public Integer getUpdatedArtistCount() {
        return updatedArtistCount;
    }

    public Integer getDeletedArtistCount() {
        return deletedArtistCount;
    }

    public Integer getCreatedAlbumCount() {
        return createdAlbumCount;
    }

    public Integer getUpdatedAlbumCount() {
        return updatedAlbumCount;
    }

    public Integer getDeletedAlbumCount() {
        return deletedAlbumCount;
    }

    public Integer getCreatedGenreCount() {
        return createdGenreCount;
    }

    public Integer getUpdatedGenreCount() {
        return updatedGenreCount;
    }

    public Integer getDeletedGenreCount() {
        return deletedGenreCount;
    }

    public Integer getCreatedSongCount() {
        return createdSongCount;
    }

    public Integer getUpdatedSongCount() {
        return updatedSongCount;
    }

    public Integer getDeletedSongCount() {
        return deletedSongCount;
    }

    public Integer getCreatedArtworkCount() {
        return createdArtworkCount;
    }

    public Integer getDeletedArtworkCount() {
        return deletedArtworkCount;
    }

    public static ScanResultDto of(ScanResult scanResult) {
        return new ScanResultDto(scanResult.getId(), scanResult.getDate(), scanResult.getScanType(),
                scanResult.getTargetPaths(), scanResult.getFailedPaths(),
                scanResult.getProcessedAudioFileCount(), scanResult.getDuration(), scanResult.getSongSize(), scanResult.getArtworkSize(),
                scanResult.getGenreCount(), scanResult.getArtistCount(), scanResult.getAlbumCount(), scanResult.getSongCount(), scanResult.getArtworkCount(),
                scanResult.getCreatedArtistCount(), scanResult.getUpdatedArtistCount(), scanResult.getDeletedArtistCount(),
                scanResult.getCreatedAlbumCount(), scanResult.getUpdatedAlbumCount(), scanResult.getDeletedAlbumCount(),
                scanResult.getCreatedGenreCount(), scanResult.getUpdatedGenreCount(), scanResult.getDeletedGenreCount(),
                scanResult.getCreatedSongCount(), scanResult.getUpdatedSongCount(), scanResult.getDeletedSongCount(),
                scanResult.getCreatedArtworkCount(), scanResult.getDeletedArtworkCount());
    }
}
