package net.dorokhov.pony.library.domain;

import com.google.common.collect.ComparisonChain;
import net.dorokhov.pony.common.SearchableEntity;
import net.dorokhov.pony.common.OptionalComparators;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

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

    public Optional<Integer> getDiscNumber() {
        return Optional.ofNullable(discNumber);
    }

    public Optional<Integer> getDiscCount() {
        return Optional.ofNullable(discCount);
    }

    public Optional<Integer> getTrackNumber() {
        return Optional.ofNullable(trackNumber);
    }

    public Optional<Integer> getTrackCount() {
        return Optional.ofNullable(trackCount);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getGenreName() {
        return Optional.ofNullable(genreName);
    }

    public Optional<String> getArtistName() {
        return Optional.ofNullable(artistName);
    }

    public Optional<String> getAlbumArtistName() {
        return Optional.ofNullable(albumArtistName);
    }

    public Optional<String> getAlbumName() {
        return Optional.ofNullable(albumName);
    }

    public Optional<Integer> getYear() {
        return Optional.ofNullable(year);
    }

    public Optional<Artwork> getArtwork() {
        return Optional.ofNullable(artwork);
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
        return getName().orElse("") + " " +
                getArtistName().orElse("") + " " +
                getAlbumArtistName().orElse("") + " " +
                getAlbumName().orElse("");
    }

    @Override
    public int compareTo(Song song) {
        return ComparisonChain.start()
                .compare(getAlbum().getArtist(), song.getAlbum().getArtist())
                .compare(getAlbum(), song.getAlbum())
                .compare(getDiscNumber(), song.getDiscNumber(), OptionalComparators.nullLast())
                .compare(getTrackNumber(), song.getTrackNumber(), OptionalComparators.nullLast())
                .compare(getName(), song.getName(), OptionalComparators.nullLast())
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

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder creationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder updateDate(LocalDateTime updateDate) {
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

        public Builder discNumber(Integer discNumber) {
            this.discNumber = discNumber;
            return this;
        }

        public Builder discCount(Integer discCount) {
            this.discCount = discCount;
            return this;
        }

        public Builder trackNumber(Integer trackNumber) {
            this.trackNumber = trackNumber;
            return this;
        }

        public Builder trackCount(Integer trackCount) {
            this.trackCount = trackCount;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder genreName(String genreName) {
            this.genreName = genreName;
            return this;
        }

        public Builder artistName(String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder albumArtistName(String albumArtistName) {
            this.albumArtistName = albumArtistName;
            return this;
        }

        public Builder albumName(String albumName) {
            this.albumName = albumName;
            return this;
        }

        public Builder year(Integer year) {
            this.year = year;
            return this;
        }

        public Builder artwork(Artwork artwork) {
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
