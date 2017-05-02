package net.dorokhov.pony.library.domain;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.dorokhov.pony.common.JsonAttributeConverter;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "scan_result")
public class ScanResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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
    private List<String> targetPaths = ImmutableList.of();

    @Column(name = "failed_paths")
    @Convert(converter = JsonAttributeConverter.class)
    @NotNull
    private List<String> failedPaths = ImmutableList.of();

    @Column(name = "audio_file_count")
    @NotNull
    private Long audioFileCount;

    @Column(name = "image_file_count")
    @NotNull
    private Long imageFileCount;

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
    private Long genreCount;

    @Column(name = "artist_count")
    @NotNull
    private Long artistCount;

    @Column(name = "album_count")
    @NotNull
    private Long albumCount;

    @Column(name = "song_count")
    @NotNull
    private Long songCount;

    @Column(name = "artwork_count")
    @NotNull
    private Long artworkCount;

    @Column(name = "created_artist_count")
    @NotNull
    private Long createdArtistCount;

    @Column(name = "updated_artist_count")
    @NotNull
    private Long updatedArtistCount;

    @Column(name = "deleted_artist_count")
    @NotNull
    private Long deletedArtistCount;

    @Column(name = "created_album_count")
    @NotNull
    private Long createdAlbumCount;

    @Column(name = "updated_album_count")
    @NotNull
    private Long updatedAlbumCount;

    @Column(name = "deleted_album_count")
    @NotNull
    private Long deletedAlbumCount;

    @Column(name = "created_genre_count")
    @NotNull
    private Long createdGenreCount;

    @Column(name = "updated_genre_count")
    @NotNull
    private Long updatedGenreCount;

    @Column(name = "deleted_genre_count")
    @NotNull
    private Long deletedGenreCount;

    @Column(name = "created_song_count")
    @NotNull
    private Long createdSongCount;

    @Column(name = "updated_song_count")
    @NotNull
    private Long updatedSongCount;

    @Column(name = "deleted_song_count")
    @NotNull
    private Long deletedSongCount;

    @Column(name = "created_artwork_count")
    @NotNull
    private Long createdArtworkCount;

    @Column(name = "deleted_artwork_count")
    @NotNull
    private Long deletedArtworkCount;

    private ScanResult(Builder builder) {
        id = builder.id;
        date = builder.date;
        scanType = checkNotNull(builder.scanType);
        targetPaths = checkNotNull(builder.targetPaths.build());
        failedPaths = checkNotNull(builder.failedPaths.build());
        audioFileCount = checkNotNull(builder.audioFileCount);
        imageFileCount = checkNotNull(builder.imageFileCount);
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
        return targetPaths != null ? ImmutableList.copyOf(targetPaths) : ImmutableList.of();
    }

    public List<String> getFailedPaths() {
        return failedPaths != null ? ImmutableList.copyOf(failedPaths) : ImmutableList.of();
    }

    public Long getAudioFileCount() {
        return audioFileCount;
    }

    public Long getImageFileCount() {
        return imageFileCount;
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

    public Long getGenreCount() {
        return genreCount;
    }

    public Long getArtistCount() {
        return artistCount;
    }

    public Long getAlbumCount() {
        return albumCount;
    }

    public Long getSongCount() {
        return songCount;
    }

    public Long getArtworkCount() {
        return artworkCount;
    }

    public Long getCreatedArtistCount() {
        return createdArtistCount;
    }

    public Long getUpdatedArtistCount() {
        return updatedArtistCount;
    }

    public Long getDeletedArtistCount() {
        return deletedArtistCount;
    }

    public Long getCreatedAlbumCount() {
        return createdAlbumCount;
    }

    public Long getUpdatedAlbumCount() {
        return updatedAlbumCount;
    }

    public Long getDeletedAlbumCount() {
        return deletedAlbumCount;
    }

    public Long getCreatedGenreCount() {
        return createdGenreCount;
    }

    public Long getUpdatedGenreCount() {
        return updatedGenreCount;
    }

    public Long getDeletedGenreCount() {
        return deletedGenreCount;
    }

    public Long getCreatedSongCount() {
        return createdSongCount;
    }

    public Long getUpdatedSongCount() {
        return updatedSongCount;
    }

    public Long getDeletedSongCount() {
        return deletedSongCount;
    }

    public Long getCreatedArtworkCount() {
        return createdArtworkCount;
    }

    public Long getDeletedArtworkCount() {
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
        return "ScanResult{" +
                "id=" + id +
                ", date=" + date +
                ", scanType=" + scanType +
                ", targetPaths=" + targetPaths +
                ", failedPaths=" + failedPaths +
                ", duration=" + duration +
                ", songSize=" + songSize +
                ", artworkSize=" + artworkSize +
                ", genreCount=" + genreCount +
                ", artistCount=" + artistCount +
                ", albumCount=" + albumCount +
                ", songCount=" + songCount +
                ", artworkCount=" + artworkCount +
                ", audioFileCount=" + audioFileCount +
                ", createdArtistCount=" + createdArtistCount +
                ", updatedArtistCount=" + updatedArtistCount +
                ", deletedArtistCount=" + deletedArtistCount +
                ", createdAlbumCount=" + createdAlbumCount +
                ", updatedAlbumCount=" + updatedAlbumCount +
                ", deletedAlbumCount=" + deletedAlbumCount +
                ", createdGenreCount=" + createdGenreCount +
                ", updatedGenreCount=" + updatedGenreCount +
                ", deletedGenreCount=" + deletedGenreCount +
                ", createdSongCount=" + createdSongCount +
                ", updatedSongCount=" + updatedSongCount +
                ", deletedSongCount=" + deletedSongCount +
                ", createdArtworkCount=" + createdArtworkCount +
                ", deletedArtworkCount=" + deletedArtworkCount +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private Long id;
        private LocalDateTime date;
        private ScanType scanType;
        private ImmutableList.Builder<String> targetPaths;
        private ImmutableList.Builder<String> failedPaths;
        private Long audioFileCount;
        private Long imageFileCount;
        private Long duration;
        private Long songSize;
        private Long artworkSize;
        private Long genreCount;
        private Long artistCount;
        private Long albumCount;
        private Long songCount;
        private Long artworkCount;
        private Long createdArtistCount;
        private Long updatedArtistCount;
        private Long deletedArtistCount;
        private Long createdAlbumCount;
        private Long updatedAlbumCount;
        private Long deletedAlbumCount;
        private Long createdGenreCount;
        private Long updatedGenreCount;
        private Long deletedGenreCount;
        private Long createdSongCount;
        private Long updatedSongCount;
        private Long deletedSongCount;
        private Long createdArtworkCount;
        private Long deletedArtworkCount;

        public Builder() {
        }

        public Builder id(@Nullable Long id) {
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

        public Builder audioFileCount(Long processedSongCount) {
            this.audioFileCount = processedSongCount;
            return this;
        }

        public Builder imageFileCount(Long imageFileCount) {
            this.imageFileCount = imageFileCount;
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

        public Builder genreCount(Long genreCount) {
            this.genreCount = genreCount;
            return this;
        }

        public Builder artistCount(Long artistCount) {
            this.artistCount = artistCount;
            return this;
        }

        public Builder albumCount(Long albumCount) {
            this.albumCount = albumCount;
            return this;
        }

        public Builder songCount(Long songCount) {
            this.songCount = songCount;
            return this;
        }

        public Builder artworkCount(Long artworkCount) {
            this.artworkCount = artworkCount;
            return this;
        }

        public Builder createdArtistCount(Long createdArtistCount) {
            this.createdArtistCount = createdArtistCount;
            return this;
        }

        public Builder updatedArtistCount(Long updatedArtistCount) {
            this.updatedArtistCount = updatedArtistCount;
            return this;
        }

        public Builder deletedArtistCount(Long deletedArtistCount) {
            this.deletedArtistCount = deletedArtistCount;
            return this;
        }

        public Builder createdAlbumCount(Long createdAlbumCount) {
            this.createdAlbumCount = createdAlbumCount;
            return this;
        }

        public Builder updatedAlbumCount(Long updatedAlbumCount) {
            this.updatedAlbumCount = updatedAlbumCount;
            return this;
        }

        public Builder deletedAlbumCount(Long deletedAlbumCount) {
            this.deletedAlbumCount = deletedAlbumCount;
            return this;
        }

        public Builder createdGenreCount(Long createdGenreCount) {
            this.createdGenreCount = createdGenreCount;
            return this;
        }

        public Builder updatedGenreCount(Long updatedGenreCount) {
            this.updatedGenreCount = updatedGenreCount;
            return this;
        }

        public Builder deletedGenreCount(Long deletedGenreCount) {
            this.deletedGenreCount = deletedGenreCount;
            return this;
        }

        public Builder createdSongCount(Long createdSongCount) {
            this.createdSongCount = createdSongCount;
            return this;
        }

        public Builder updatedSongCount(Long updatedSongCount) {
            this.updatedSongCount = updatedSongCount;
            return this;
        }

        public Builder deletedSongCount(Long deletedSongCount) {
            this.deletedSongCount = deletedSongCount;
            return this;
        }

        public Builder createdArtworkCount(Long createdArtworkCount) {
            this.createdArtworkCount = createdArtworkCount;
            return this;
        }

        public Builder deletedArtworkCount(Long deletedArtworkCount) {
            this.deletedArtworkCount = deletedArtworkCount;
            return this;
        }

        public ScanResult build() {
            return new ScanResult(this);
        }
    }
}
