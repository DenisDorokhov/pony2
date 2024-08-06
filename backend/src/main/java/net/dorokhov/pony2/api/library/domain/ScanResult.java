package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.common.ListOfStringsJsonAttributeConverter;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scan_result")
public class ScanResult implements Serializable {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @Column(name = "date")
    @NotNull
    private LocalDateTime date;

    @Column(name = "scan_type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private ScanType scanType;

    @Column(name = "failed_paths")
    @Convert(converter = ListOfStringsJsonAttributeConverter.class)
    @NotNull
    private List<String> failedPaths = new ArrayList<>();

    @Column(name = "processed_audio_file_count")
    @NotNull
    private Integer processedAudioFileCount;

    @Column(name = "duration")
    @NotNull
    private Long duration;

    @Column(name = "song_size")
    @NotNull
    private Long songSize;

    @Column(name = "artwork_size")
    @NotNull
    private Long artworkSize;

    @Column(name = "genre_count")
    @NotNull
    private Integer genreCount;

    @Column(name = "artist_count")
    @NotNull
    private Integer artistCount;

    @Column(name = "album_count")
    @NotNull
    private Integer albumCount;

    @Column(name = "song_count")
    @NotNull
    private Integer songCount;

    @Column(name = "artwork_count")
    @NotNull
    private Integer artworkCount;

    @Column(name = "created_artist_count")
    @NotNull
    private Integer createdArtistCount;

    @Column(name = "updated_artist_count")
    @NotNull
    private Integer updatedArtistCount;

    @Column(name = "deleted_artist_count")
    @NotNull
    private Integer deletedArtistCount;

    @Column(name = "created_album_count")
    @NotNull
    private Integer createdAlbumCount;

    @Column(name = "updated_album_count")
    @NotNull
    private Integer updatedAlbumCount;

    @Column(name = "deleted_album_count")
    @NotNull
    private Integer deletedAlbumCount;

    @Column(name = "created_genre_count")
    @NotNull
    private Integer createdGenreCount;

    @Column(name = "updated_genre_count")
    @NotNull
    private Integer updatedGenreCount;

    @Column(name = "deleted_genre_count")
    @NotNull
    private Integer deletedGenreCount;

    @Column(name = "created_song_count")
    @NotNull
    private Integer createdSongCount;

    @Column(name = "updated_song_count")
    @NotNull
    private Integer updatedSongCount;

    @Column(name = "deleted_song_count")
    @NotNull
    private Integer deletedSongCount;

    @Column(name = "created_artwork_count")
    @NotNull
    private Integer createdArtworkCount;

    @Column(name = "deleted_artwork_count")
    @NotNull
    private Integer deletedArtworkCount;

    public String getId() {
        return id;
    }

    public ScanResult setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public ScanResult setDate(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public ScanResult setScanType(ScanType scanType) {
        this.scanType = scanType;
        return this;
    }

    public List<String> getFailedPaths() {
        if (failedPaths == null) {
            failedPaths = new ArrayList<>();
        }
        return failedPaths;
    }

    public ScanResult setFailedPaths(List<String> failedPaths) {
        this.failedPaths = failedPaths;
        return this;
    }

    public Integer getProcessedAudioFileCount() {
        return processedAudioFileCount;
    }

    public ScanResult setProcessedAudioFileCount(Integer processedAudioFileCount) {
        this.processedAudioFileCount = processedAudioFileCount;
        return this;
    }

    public Long getDuration() {
        return duration;
    }

    public ScanResult setDuration(Long duration) {
        this.duration = duration;
        return this;
    }

    public Long getSongSize() {
        return songSize;
    }

    public ScanResult setSongSize(Long songSize) {
        this.songSize = songSize;
        return this;
    }

    public Long getArtworkSize() {
        return artworkSize;
    }

    public ScanResult setArtworkSize(Long artworkSize) {
        this.artworkSize = artworkSize;
        return this;
    }

    public Integer getGenreCount() {
        return genreCount;
    }

    public ScanResult setGenreCount(Integer genreCount) {
        this.genreCount = genreCount;
        return this;
    }

    public Integer getArtistCount() {
        return artistCount;
    }

    public ScanResult setArtistCount(Integer artistCount) {
        this.artistCount = artistCount;
        return this;
    }

    public Integer getAlbumCount() {
        return albumCount;
    }

    public ScanResult setAlbumCount(Integer albumCount) {
        this.albumCount = albumCount;
        return this;
    }

    public Integer getSongCount() {
        return songCount;
    }

    public ScanResult setSongCount(Integer songCount) {
        this.songCount = songCount;
        return this;
    }

    public Integer getArtworkCount() {
        return artworkCount;
    }

    public ScanResult setArtworkCount(Integer artworkCount) {
        this.artworkCount = artworkCount;
        return this;
    }

    public Integer getCreatedArtistCount() {
        return createdArtistCount;
    }

    public ScanResult setCreatedArtistCount(Integer createdArtistCount) {
        this.createdArtistCount = createdArtistCount;
        return this;
    }

    public Integer getUpdatedArtistCount() {
        return updatedArtistCount;
    }

    public ScanResult setUpdatedArtistCount(Integer updatedArtistCount) {
        this.updatedArtistCount = updatedArtistCount;
        return this;
    }

    public Integer getDeletedArtistCount() {
        return deletedArtistCount;
    }

    public ScanResult setDeletedArtistCount(Integer deletedArtistCount) {
        this.deletedArtistCount = deletedArtistCount;
        return this;
    }

    public Integer getCreatedAlbumCount() {
        return createdAlbumCount;
    }

    public ScanResult setCreatedAlbumCount(Integer createdAlbumCount) {
        this.createdAlbumCount = createdAlbumCount;
        return this;
    }

    public Integer getUpdatedAlbumCount() {
        return updatedAlbumCount;
    }

    public ScanResult setUpdatedAlbumCount(Integer updatedAlbumCount) {
        this.updatedAlbumCount = updatedAlbumCount;
        return this;
    }

    public Integer getDeletedAlbumCount() {
        return deletedAlbumCount;
    }

    public ScanResult setDeletedAlbumCount(Integer deletedAlbumCount) {
        this.deletedAlbumCount = deletedAlbumCount;
        return this;
    }

    public Integer getCreatedGenreCount() {
        return createdGenreCount;
    }

    public ScanResult setCreatedGenreCount(Integer createdGenreCount) {
        this.createdGenreCount = createdGenreCount;
        return this;
    }

    public Integer getUpdatedGenreCount() {
        return updatedGenreCount;
    }

    public ScanResult setUpdatedGenreCount(Integer updatedGenreCount) {
        this.updatedGenreCount = updatedGenreCount;
        return this;
    }

    public Integer getDeletedGenreCount() {
        return deletedGenreCount;
    }

    public ScanResult setDeletedGenreCount(Integer deletedGenreCount) {
        this.deletedGenreCount = deletedGenreCount;
        return this;
    }

    public Integer getCreatedSongCount() {
        return createdSongCount;
    }

    public ScanResult setCreatedSongCount(Integer createdSongCount) {
        this.createdSongCount = createdSongCount;
        return this;
    }

    public Integer getUpdatedSongCount() {
        return updatedSongCount;
    }

    public ScanResult setUpdatedSongCount(Integer updatedSongCount) {
        this.updatedSongCount = updatedSongCount;
        return this;
    }

    public Integer getDeletedSongCount() {
        return deletedSongCount;
    }

    public ScanResult setDeletedSongCount(Integer deletedSongCount) {
        this.deletedSongCount = deletedSongCount;
        return this;
    }

    public Integer getCreatedArtworkCount() {
        return createdArtworkCount;
    }

    public ScanResult setCreatedArtworkCount(Integer createdArtworkCount) {
        this.createdArtworkCount = createdArtworkCount;
        return this;
    }

    public Integer getDeletedArtworkCount() {
        return deletedArtworkCount;
    }

    public ScanResult setDeletedArtworkCount(Integer deletedArtworkCount) {
        this.deletedArtworkCount = deletedArtworkCount;
        return this;
    }

    @PrePersist
    public void prePersist() {
        date = LocalDateTime.now();
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

    @Override
    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && id != null && getClass().equals(obj.getClass())) {
            ScanResult that = (ScanResult) obj;
            return id.equals(that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("date", date)
                .add("scanType", scanType)
                .add("failedPaths", failedPaths)
                .add("processedAudioFileCount", processedAudioFileCount)
                .add("duration", duration)
                .add("songSize", songSize)
                .add("artworkSize", artworkSize)
                .add("genreCount", genreCount)
                .add("artistCount", artistCount)
                .add("albumCount", albumCount)
                .add("songCount", songCount)
                .add("artworkCount", artworkCount)
                .add("createdArtistCount", createdArtistCount)
                .add("updatedArtistCount", updatedArtistCount)
                .add("deletedArtistCount", deletedArtistCount)
                .add("createdAlbumCount", createdAlbumCount)
                .add("updatedAlbumCount", updatedAlbumCount)
                .add("deletedAlbumCount", deletedAlbumCount)
                .add("createdGenreCount", createdGenreCount)
                .add("updatedGenreCount", updatedGenreCount)
                .add("deletedGenreCount", deletedGenreCount)
                .add("createdSongCount", createdSongCount)
                .add("updatedSongCount", updatedSongCount)
                .add("deletedSongCount", deletedSongCount)
                .add("createdArtworkCount", createdArtworkCount)
                .add("deletedArtworkCount", deletedArtworkCount)
                .toString();
    }
}
