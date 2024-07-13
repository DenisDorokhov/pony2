package net.dorokhov.pony3.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony3.api.common.BaseEntity;

import java.io.File;
import java.io.Serializable;

@Entity
@Table(name = "song")
public class Song extends BaseEntity<Song> implements Comparable<Song>, Serializable {

    @Column(name = "path", nullable = false, unique = true)
    @NotNull
    private String path;

    @Column(name = "mime_type", nullable = false)
    @NotNull
    private String mimeType;

    @Column(name = "file_extension", nullable = false)
    @NotNull
    private String fileExtension;

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

    @Column(name = "song_year")
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    @NotNull
    private Album album;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    @NotNull
    private Genre genre;

    @Transient
    private FileType fileType;

    public String getPath() {
        return path;
    }

    public Song setPath(String path) {
        this.path = path;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Song setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public Song setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public Song setSize(Long size) {
        this.size = size;
        return this;
    }

    public Long getDuration() {
        return duration;
    }

    public Song setDuration(Long duration) {
        this.duration = duration;
        return this;
    }

    public Long getBitRate() {
        return bitRate;
    }

    public Song setBitRate(Long bitRate) {
        this.bitRate = bitRate;
        return this;
    }

    public Boolean getBitRateVariable() {
        return bitRateVariable;
    }

    public Song setBitRateVariable(Boolean bitRateVariable) {
        this.bitRateVariable = bitRateVariable;
        return this;
    }

    public @Nullable Integer getDiscNumber() {
        return discNumber;
    }

    public Song setDiscNumber(@Nullable Integer discNumber) {
        this.discNumber = discNumber;
        return this;
    }

    public @Nullable Integer getDiscCount() {
        return discCount;
    }

    public Song setDiscCount(@Nullable Integer discCount) {
        this.discCount = discCount;
        return this;
    }

    public @Nullable Integer getTrackNumber() {
        return trackNumber;
    }

    public Song setTrackNumber(@Nullable Integer trackNumber) {
        this.trackNumber = trackNumber;
        return this;
    }

    public @Nullable Integer getTrackCount() {
        return trackCount;
    }

    public Song setTrackCount(@Nullable Integer trackCount) {
        this.trackCount = trackCount;
        return this;
    }

    public @Nullable String getName() {
        return name;
    }

    public Song setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    public @Nullable String getGenreName() {
        return genreName;
    }

    public Song setGenreName(@Nullable String genreName) {
        this.genreName = genreName;
        return this;
    }

    public @Nullable String getArtistName() {
        return artistName;
    }

    public Song setArtistName(@Nullable String artistName) {
        this.artistName = artistName;
        return this;
    }

    public @Nullable String getAlbumArtistName() {
        return albumArtistName;
    }

    public Song setAlbumArtistName(@Nullable String albumArtistName) {
        this.albumArtistName = albumArtistName;
        return this;
    }

    public @Nullable String getAlbumName() {
        return albumName;
    }

    public Song setAlbumName(@Nullable String albumName) {
        this.albumName = albumName;
        return this;
    }

    public @Nullable Integer getYear() {
        return year;
    }

    public Song setYear(@Nullable Integer year) {
        this.year = year;
        return this;
    }

    public @Nullable Artwork getArtwork() {
        return artwork;
    }

    public Song setArtwork(@Nullable Artwork artwork) {
        this.artwork = artwork;
        return this;
    }

    public Album getAlbum() {
        return album;
    }

    public Song setAlbum(Album album) {
        this.album = album;
        return this;
    }

    public Genre getGenre() {
        return genre;
    }

    public Song setGenre(Genre genre) {
        this.genre = genre;
        return this;
    }

    public FileType getFileType() {
        return fileType;
    }

    public Song setFileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    @Transient
    public String getSearchTerms() {
        return Strings.nullToEmpty(name) + " " +
                Strings.nullToEmpty(artistName) + " " +
                Strings.nullToEmpty(albumArtistName) + " " +
                Strings.nullToEmpty(albumName);
    }

    @Transient
    public File getFile() {
        return new File(getPath());
    }

    @PostLoad
    public void postLoad() {
        fileType = FileType.of(mimeType, fileExtension);
    }

    @Override
    public int compareTo(Song song) {
        return ComparisonChain.start()
                .compare(getAlbum().getArtist(), song.getAlbum().getArtist())
                .compare(getAlbum(), song.getAlbum())
                .compare(discNumber, song.discNumber, (disc1, disc2) -> {
                    int normalizedDisc1 = disc1 == null || disc1 < 1 ? 1 : disc1;
                    int normalizedDisc2 = disc2 == null || disc2 < 1 ? 1 : disc2;
                    return Integer.compare(normalizedDisc1, normalizedDisc2);
                })
                .compare(trackNumber, song.trackNumber, Ordering.natural().nullsLast())
                .compare(name, song.name, Ordering.natural().nullsLast())
                .result();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("path", path)
                .add("fileType", fileType)
                .add("name", name)
                .add("album", album)
                .add("genre", genre)
                .toString();
    }
}
