package net.dorokhov.pony.library.domain;

import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import net.dorokhov.pony.common.SearchableEntity;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "song")
@Indexed
public class Song extends SearchableEntity<Long> implements Comparable<Song>, Serializable {

    @Column(name = "path", nullable = false, unique = true)
    @NotNull
    private String path;

    @Column(name = "mime_type", nullable = false)
    @NotNull
    private String mimeType;

    @Column(name = "file_extension", nullable = false)
    @NotNull
    private String fileExtension;

    @Transient
    private FileType fileType;

    @Column(name = "size", nullable = false)
    @NotNull
    private Long size;

    @Column(name = "duration", nullable = false)
    @NotNull
    private Long duration;

    @Column(name = "bit_rate", nullable = false)
    @NotNull
    private Long bitRate;

    @Column(name = "bit_rate_variable", nullable = false)
    @NotNull
    private Boolean bitRateVariable;

    @Column(name = "disc_number")
    private Integer discNumber;

    @Column(name = "disc_count")
    private Integer discCount;

    @Column(name = "track_number")
    private Integer trackNumber;

    @Column(name = "track_count")
    private Integer trackCount;

    @Column(name = "name")
    private String name;

    @Column(name = "genre_name")
    private String genreName;

    @Column(name = "artist_name")
    private String artistName;

    @Column(name = "album_artist_name")
    private String albumArtistName;

    @Column(name = "album_name")
    private String albumName;

    @Column(name = "year")
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    protected Song() {
    }

    private Song(Builder builder) {
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
        path = checkNotNull(builder.path);
        mimeType = builder.fileType.getMimeType();
        fileExtension = builder.fileType.getFileExtension();
        fileType = FileType.of(mimeType, fileExtension);
        size = checkNotNull(builder.size);
        duration = checkNotNull(builder.duration);
        bitRate = checkNotNull(builder.bitRate);
        bitRateVariable = checkNotNull(builder.bitRateVariable);
        discNumber = builder.discNumber;
        discCount = builder.discCount;
        trackNumber = builder.trackNumber;
        trackCount = builder.trackCount;
        name = builder.name;
        genreName = builder.genreName;
        artistName = builder.artistName;
        albumArtistName = builder.albumArtistName;
        albumName = builder.albumName;
        year = builder.year;
        artwork = builder.artwork;
        album = checkNotNull(builder.album);
        genre = checkNotNull(builder.genre);
    }

    public String getPath() {
        return path;
    }

    public FileType getFileType() {
        return fileType;
    }

    public long getSize() {
        return size;
    }

    public long getDuration() {
        return duration;
    }

    public long getBitRate() {
        return bitRate;
    }

    public boolean getBitRateVariable() {
        return bitRateVariable;
    }

    @Nullable
    public Integer getDiscNumber() {
        return discNumber;
    }

    @Nullable
    public Integer getDiscCount() {
        return discCount;
    }

    @Nullable
    public Integer getTrackNumber() {
        return trackNumber;
    }

    @Nullable
    public Integer getTrackCount() {
        return trackCount;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getGenreName() {
        return genreName;
    }

    @Nullable
    public String getArtistName() {
        return artistName;
    }

    @Nullable
    public String getAlbumArtistName() {
        return albumArtistName;
    }

    @Nullable
    public String getAlbumName() {
        return albumName;
    }

    @Nullable
    public Integer getYear() {
        return year;
    }

    @Nullable
    public Artwork getArtwork() {
        return artwork;
    }

    public Album getAlbum() {
        return album;
    }

    public Genre getGenre() {
        return genre;
    }

    @Transient
    @Field(analyzer = @Analyzer(definition = ANALYZER))
    public String getSearchTerms() {
        return Strings.nullToEmpty(name) + " " +
                Strings.nullToEmpty(artistName) + " " +
                Strings.nullToEmpty(albumArtistName) + " " +
                Strings.nullToEmpty(albumName);
    }

    @Override
    public int compareTo(@Nonnull Song song) {
        return ComparisonChain.start()
                .compare(album.getArtist(), song.album.getArtist())
                .compare(album, song.album)
                .compare(discNumber, song.discNumber, Ordering.natural().nullsLast())
                .compare(trackNumber, song.trackNumber, Ordering.natural().nullsLast())
                .compare(name, song.name, Ordering.natural().nullsLast())
                .result();
    }

    @Override
    public String toString() {
        return "Song{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", fileType='" + fileType + '\'' +
                ", name='" + name + '\'' +
                ", album=" + album +
                ", genre=" + genre +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Song song) {
        return new Builder(song);
    }

    public static class Builder {
        
        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String path;
        private FileType fileType;
        private Long size;
        private Long duration;
        private Long bitRate;
        private Boolean bitRateVariable;
        private Integer discNumber;
        private Integer discCount;
        private Integer trackNumber;
        private Integer trackCount;
        private String name;
        private String genreName;
        private String artistName;
        private String albumArtistName;
        private String albumName;
        private Integer year;
        private Artwork artwork;
        private Album album;
        private Genre genre;

        public Builder() {
        }
        
        public Builder(Song song) {
            path = song.path;
            fileType = song.fileType;
            size = song.size;
            duration = song.duration;
            bitRate = song.bitRate;
            bitRateVariable = song.bitRateVariable;
            discNumber = song.discNumber;
            discCount = song.discCount;
            id = song.id;
            trackNumber = song.trackNumber;
            trackCount = song.trackCount;
            creationDate = song.creationDate;
            name = song.name;
            genreName = song.genreName;
            updateDate = song.updateDate;
            artistName = song.artistName;
            albumArtistName = song.albumArtistName;
            albumName = song.albumName;
            year = song.year;
            artwork = song.artwork;
            album = song.album;
            genre = song.genre;
        }

        public Builder id(@Nullable Long id) {
            this.id = id;
            return this;
        }

        public Builder creationDate(@Nullable LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder updateDate(@Nullable LocalDateTime updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder fileType(FileType fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        public Builder duration(Long duration) {
            this.duration = duration;
            return this;
        }

        public Builder bitRate(Long bitRate) {
            this.bitRate = bitRate;
            return this;
        }

        public Builder bitRateVariable(Boolean bitRateVariable) {
            this.bitRateVariable = bitRateVariable;
            return this;
        }

        public Builder discNumber(@Nullable Integer discNumber) {
            this.discNumber = discNumber;
            return this;
        }

        public Builder discCount(@Nullable Integer discCount) {
            this.discCount = discCount;
            return this;
        }

        public Builder trackNumber(@Nullable Integer trackNumber) {
            this.trackNumber = trackNumber;
            return this;
        }

        public Builder trackCount(@Nullable Integer trackCount) {
            this.trackCount = trackCount;
            return this;
        }

        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        public Builder genreName(@Nullable String genreName) {
            this.genreName = genreName;
            return this;
        }

        public Builder artistName(@Nullable String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder albumArtistName(@Nullable String albumArtistName) {
            this.albumArtistName = albumArtistName;
            return this;
        }

        public Builder albumName(@Nullable String albumName) {
            this.albumName = albumName;
            return this;
        }

        public Builder year(@Nullable Integer year) {
            this.year = year;
            return this;
        }

        public Builder artwork(@Nullable Artwork artwork) {
            this.artwork = artwork;
            return this;
        }

        public Builder album(Album album) {
            this.album = album;
            return this;
        }

        public Builder genre(Genre genre) {
            this.genre = genre;
            return this;
        }

        public Song build() {
            return new Song(this);
        }
    }
}
