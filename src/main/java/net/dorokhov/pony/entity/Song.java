package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import net.dorokhov.pony.util.OptionalComparator;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Entity
@Table(name = "song")
@Indexed
public class Song extends BaseEntity<Long> implements Comparable<Song> {

    @Column(name = "path", nullable = false, unique = true)
    @NotNull
    private String path;

    @Column(name = "mime_type", nullable = false)
    @NotNull
    private String mimeType;

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

    public Song() {
    }

    public Song(Album album, Genre genre) {
        setAlbum(album);
        setGenre(genre);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getBitRate() {
        return bitRate;
    }

    public void setBitRate(Long bitRate) {
        this.bitRate = bitRate;
    }

    public Boolean getBitRateVariable() {
        return bitRateVariable;
    }

    public void setBitRateVariable(Boolean bitRateVariable) {
        this.bitRateVariable = bitRateVariable;
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

    public Optional<Artwork> getArtwork() {
        return Optional.ofNullable(artwork);
    }

    public void setArtwork(Artwork artwork) {
        this.artwork = artwork;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    @Transient
    @Field
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
                .add("path", path)
                .add("name", name)
                .add("album", album)
                .add("genre", genre)
                .toString();
    }
}
