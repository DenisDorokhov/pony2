package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import net.dorokhov.pony.search.SearchAnalyzer;
import net.dorokhov.pony.util.OptionalComparator;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "song")
@Indexed
public class Song extends BaseEntity<Long> implements Comparable<Song> {

    @Column(name = "path", nullable = false, unique = true)
    private String path;

    @Column(name = "format", nullable = false)
    private String format;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "bit_rate", nullable = false)
    private Long bitRate;

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
    @JoinColumn(name = "artwork_stored_file_id")
    private StoredFile artwork;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    private Song() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = Preconditions.checkNotNull(path);
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String type) {
        format = Preconditions.checkNotNull(type);
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = Preconditions.checkNotNull(mimeType);
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = Preconditions.checkNotNull(size);
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = Preconditions.checkNotNull(duration);
    }

    public Long getBitRate() {
        return bitRate;
    }

    public void setBitRate(Long bitRate) {
        this.bitRate = Preconditions.checkNotNull(bitRate);
    }

    public Optional<Integer> getDiscNumber() {
        return Optional.ofNullable(discNumber);
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    public Optional<Integer> getDiscCount() {
        return Optional.ofNullable(discCount);
    }

    public void setDiscCount(Integer discCount) {
        this.discCount = discCount;
    }

    public Optional<Integer> getTrackNumber() {
        return Optional.ofNullable(trackNumber);
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public Optional<Integer> getTrackCount() {
        return Optional.ofNullable(trackCount);
    }

    public void setTrackCount(Integer trackCount) {
        this.trackCount = trackCount;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getGenreName() {
        return Optional.ofNullable(genreName);
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public Optional<String> getArtistName() {
        return Optional.ofNullable(artistName);
    }

    public void setArtistName(String artist) {
        artistName = artist;
    }

    public Optional<String> getAlbumArtistName() {
        return Optional.ofNullable(albumArtistName);
    }

    public void setAlbumArtistName(String albumArtist) {
        albumArtistName = albumArtist;
    }

    public Optional<String> getAlbumName() {
        return Optional.ofNullable(albumName);
    }

    public void setAlbumName(String album) {
        albumName = album;
    }

    public Optional<Integer> getYear() {
        return Optional.ofNullable(year);
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Optional<StoredFile> getArtwork() {
        return Optional.ofNullable(artwork);
    }

    public void setArtwork(StoredFile artwork) {
        this.artwork = artwork;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = Preconditions.checkNotNull(album);
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = Preconditions.checkNotNull(genre);
    }

    @Transient
    @Field(analyzer = @Analyzer(impl = SearchAnalyzer.class))
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
                .compare(getDiscNumber(), song.getDiscNumber(), OptionalComparator.nullLast())
                .compare(getTrackNumber(), song.getTrackNumber(), OptionalComparator.nullLast())
                .compare(getName(), song.getName(), OptionalComparator.nullLast())
                .result();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("path", path)
                .add("format", format)
                .add("mimeType", mimeType)
                .add("size", size)
                .add("duration", duration)
                .add("bitRate", bitRate)
                .add("discNumber", discNumber)
                .add("discCount", discCount)
                .add("trackNumber", trackNumber)
                .add("trackCount", trackCount)
                .add("name", name)
                .add("genreName", genreName)
                .add("artistName", artistName)
                .add("albumArtistName", albumArtistName)
                .add("albumName", albumName)
                .add("year", year)
                .add("artwork", artwork)
                .add("album", album)
                .add("genre", genre)
                .toString();
    }

    public static class Builder {

        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;

        private String path;
        private String format;
        private String mimeType;
        private Long size;
        private Integer duration;
        private Long bitRate;
        private Album album;
        private Genre genre;

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
        private StoredFile artwork;

        public Builder() {
        }

        public Builder(Song song) {
            this.id = song.id;
            this.creationDate = song.creationDate;
            this.updateDate = song.updateDate;
            this.path = song.path;
            this.format = song.format;
            this.mimeType = song.mimeType;
            this.size = song.size;
            this.duration = song.duration;
            this.bitRate = song.bitRate;
            this.discNumber = song.discNumber;
            this.discCount = song.discCount;
            this.trackNumber = song.trackNumber;
            this.trackCount = song.trackCount;
            this.name = song.name;
            this.genreName = song.genreName;
            this.artistName = song.artistName;
            this.albumArtistName = song.albumArtistName;
            this.albumName = song.albumName;
            this.year = song.year;
            this.artwork = song.artwork;
            this.album = song.album;
            this.genre = song.genre;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setFormat(String format) {
            this.format = format;
            return this;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder setSize(Long size) {
            this.size = size;
            return this;
        }

        public Builder setDuration(Integer duration) {
            this.duration = duration;
            return this;
        }

        public Builder setBitRate(Long bitRate) {
            this.bitRate = bitRate;
            return this;
        }

        public Builder setDiscNumber(Integer discNumber) {
            this.discNumber = discNumber;
            return this;
        }

        public Builder setDiscCount(Integer discCount) {
            this.discCount = discCount;
            return this;
        }

        public Builder setTrackNumber(Integer trackNumber) {
            this.trackNumber = trackNumber;
            return this;
        }

        public Builder setTrackCount(Integer trackCount) {
            this.trackCount = trackCount;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setGenreName(String genreName) {
            this.genreName = genreName;
            return this;
        }

        public Builder setArtistName(String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder setAlbumArtistName(String albumArtistName) {
            this.albumArtistName = albumArtistName;
            return this;
        }

        public Builder setAlbumName(String albumName) {
            this.albumName = albumName;
            return this;
        }

        public Builder setYear(Integer year) {
            this.year = year;
            return this;
        }

        public Builder setArtwork(StoredFile artwork) {
            this.artwork = artwork;
            return this;
        }

        public Builder setAlbum(Album album) {
            this.album = album;
            return this;
        }

        public Builder setGenre(Genre genre) {
            this.genre = genre;
            return this;
        }

        public Song build() {
            Song song = new Song();
            song.setId(id);
            song.setCreationDate(creationDate);
            song.setUpdateDate(updateDate);
            song.setPath(path);
            song.setFormat(format);
            song.setMimeType(mimeType);
            song.setSize(size);
            song.setDuration(duration);
            song.setBitRate(bitRate);
            song.setAlbum(album);
            song.setGenre(genre);
            song.setDiscNumber(discNumber);
            song.setDiscCount(discCount);
            song.setTrackNumber(trackNumber);
            song.setTrackCount(trackCount);
            song.setName(name);
            song.setGenreName(genreName);
            song.setArtistName(artistName);
            song.setAlbumArtistName(albumArtistName);
            song.setAlbumName(albumName);
            song.setYear(year);
            song.setArtwork(artwork);
            return song;
        }
    }
}
