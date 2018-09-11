package net.dorokhov.pony.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.dorokhov.pony.common.JsonAttributeConverter;
import org.hibernate.annotations.GenericGenerator;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

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

    @Column(name = "target_paths")
    @Convert(converter = JsonAttributeConverter.class)
    @NotNull
    private List<String> targetPaths = emptyList();

    @Column(name = "failed_paths")
    @Convert(converter = JsonAttributeConverter.class)
    @NotNull
    private List<String> failedPaths = emptyList();

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

    protected ScanResult() {
    }

    private ScanResult(Builder builder) {
        id = builder.id;
        date = builder.date;
        scanType = checkNotNull(builder.scanType);
        targetPaths = builder.targetPaths.build();
        failedPaths = builder.failedPaths.build();
        processedAudioFileCount = checkNotNull(builder.processedAudioFileCount);
        duration = checkNotNull(builder.duration);
        songSize = checkNotNull(builder.songSize);
        artworkSize = checkNotNull(builder.artworkSize);
        genreCount = checkNotNull(builder.genreCount);
        artistCount = checkNotNull(builder.artistCount);
        albumCount = checkNotNull(builder.albumCount);
        songCount = checkNotNull(builder.songCount);
        artworkCount = checkNotNull(builder.artworkCount);
        createdArtistCount = checkNotNull(builder.createdArtistCount);
        updatedArtistCount = checkNotNull(builder.updatedArtistCount);
        deletedArtistCount = checkNotNull(builder.deletedArtistCount);
        createdAlbumCount = checkNotNull(builder.createdAlbumCount);
        updatedAlbumCount = checkNotNull(builder.updatedAlbumCount);
        deletedAlbumCount = checkNotNull(builder.deletedAlbumCount);
        createdGenreCount = checkNotNull(builder.createdGenreCount);
        updatedGenreCount = checkNotNull(builder.updatedGenreCount);
        deletedGenreCount = checkNotNull(builder.deletedGenreCount);
        createdSongCount = checkNotNull(builder.createdSongCount);
        updatedSongCount = checkNotNull(builder.updatedSongCount);
        deletedSongCount = checkNotNull(builder.deletedSongCount);
        createdArtworkCount = checkNotNull(builder.createdArtworkCount);
        deletedArtworkCount = checkNotNull(builder.deletedArtworkCount);
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public List<String> getTargetPaths() {
        return targetPaths != null ? unmodifiableList(targetPaths) : emptyList();
    }

    public List<String> getFailedPaths() {
        return failedPaths != null ? unmodifiableList(failedPaths) : emptyList();
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
    public boolean equals(@Nullable Object obj) {
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
                .add("targetPaths", targetPaths)
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private String id;
        private LocalDateTime date;
        private ScanType scanType;
        private ImmutableList.Builder<String> targetPaths = ImmutableList.builder();
        private ImmutableList.Builder<String> failedPaths = ImmutableList.builder();
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

        private Builder() {
        }

        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        public Builder date(@Nullable LocalDateTime date) {
            this.date = date;
            return this;
        }

        public Builder scanType(ScanType type) {
            this.scanType = type;
            return this;
        }

        public Builder targetPaths(@Nullable List<String> targetPaths) {
            if (targetPaths != null) {
                this.targetPaths = ImmutableList.<String>builder().addAll(targetPaths);
            } else {
                this.targetPaths = ImmutableList.builder();
            }
            return this;
        }

        public Builder failedPaths(@Nullable List<String> failedPaths) {
            if (failedPaths != null) {
                this.failedPaths = ImmutableList.<String>builder().addAll(failedPaths);
            } else {
                this.failedPaths = ImmutableList.builder();
            }
            return this;
        }

        public Builder processedAudioFileCount(Integer processedAudioFileCount) {
            this.processedAudioFileCount = processedAudioFileCount;
            return this;
        }

        public Builder duration(Long duration) {
            this.duration = duration;
            return this;
        }

        public Builder songSize(Long songSize) {
            this.songSize = songSize;
            return this;
        }

        public Builder artworkSize(Long artworkSize) {
            this.artworkSize = artworkSize;
            return this;
        }

        public Builder genreCount(Integer genreCount) {
            this.genreCount = genreCount;
            return this;
        }

        public Builder artistCount(Integer artistCount) {
            this.artistCount = artistCount;
            return this;
        }

        public Builder albumCount(Integer albumCount) {
            this.albumCount = albumCount;
            return this;
        }

        public Builder songCount(Integer songCount) {
            this.songCount = songCount;
            return this;
        }

        public Builder artworkCount(Integer artworkCount) {
            this.artworkCount = artworkCount;
            return this;
        }

        public Builder createdArtistCount(Integer createdArtistCount) {
            this.createdArtistCount = createdArtistCount;
            return this;
        }

        public Builder updatedArtistCount(Integer updatedArtistCount) {
            this.updatedArtistCount = updatedArtistCount;
            return this;
        }

        public Builder deletedArtistCount(Integer deletedArtistCount) {
            this.deletedArtistCount = deletedArtistCount;
            return this;
        }

        public Builder createdAlbumCount(Integer createdAlbumCount) {
            this.createdAlbumCount = createdAlbumCount;
            return this;
        }

        public Builder updatedAlbumCount(Integer updatedAlbumCount) {
            this.updatedAlbumCount = updatedAlbumCount;
            return this;
        }

        public Builder deletedAlbumCount(Integer deletedAlbumCount) {
            this.deletedAlbumCount = deletedAlbumCount;
            return this;
        }

        public Builder createdGenreCount(Integer createdGenreCount) {
            this.createdGenreCount = createdGenreCount;
            return this;
        }

        public Builder updatedGenreCount(Integer updatedGenreCount) {
            this.updatedGenreCount = updatedGenreCount;
            return this;
        }

        public Builder deletedGenreCount(Integer deletedGenreCount) {
            this.deletedGenreCount = deletedGenreCount;
            return this;
        }

        public Builder createdSongCount(Integer createdSongCount) {
            this.createdSongCount = createdSongCount;
            return this;
        }

        public Builder updatedSongCount(Integer updatedSongCount) {
            this.updatedSongCount = updatedSongCount;
            return this;
        }

        public Builder deletedSongCount(Integer deletedSongCount) {
            this.deletedSongCount = deletedSongCount;
            return this;
        }

        public Builder createdArtworkCount(Integer createdArtworkCount) {
            this.createdArtworkCount = createdArtworkCount;
            return this;
        }

        public Builder deletedArtworkCount(Integer deletedArtworkCount) {
            this.deletedArtworkCount = deletedArtworkCount;
            return this;
        }

        public ScanResult build() {
            return new ScanResult(this);
        }
    }
}
