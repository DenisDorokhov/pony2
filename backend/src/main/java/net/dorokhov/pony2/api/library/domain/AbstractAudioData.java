package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

@SuppressWarnings("unchecked")
abstract class AbstractAudioData<T extends AbstractAudioData<?>> {

    protected Integer discNumber;
    protected Integer discCount;
    protected Integer trackNumber;
    protected Integer trackCount;
    protected String title;
    protected String artist;
    protected String albumArtist;
    protected String album;
    protected Integer year;
    protected String genre;

    public @Nullable Integer getDiscNumber() {
        return discNumber;
    }

    public T setDiscNumber(@Nullable Integer discNumber) {
        this.discNumber = discNumber;
        return (T) this;
    }

    public @Nullable Integer getDiscCount() {
        return discCount;
    }

    public T setDiscCount(Integer discCount) {
        this.discCount = discCount;
        return (T) this;
    }

    public @Nullable Integer getTrackNumber() {
        return trackNumber;
    }

    public T setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
        return (T) this;
    }

    public @Nullable Integer getTrackCount() {
        return trackCount;
    }

    public T setTrackCount(Integer trackCount) {
        this.trackCount = trackCount;
        return (T) this;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public T setTitle(String title) {
        this.title = title;
        return (T) this;
    }

    public @Nullable String getArtist() {
        return artist;
    }

    public T setArtist(String artist) {
        this.artist = artist;
        return (T) this;
    }

    public @Nullable String getAlbumArtist() {
        return albumArtist;
    }

    public T setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
        return (T) this;
    }

    public @Nullable String getAlbum() {
        return album;
    }

    public T setAlbum(String album) {
        this.album = album;
        return (T) this;
    }

    public @Nullable Integer getYear() {
        return year;
    }

    public T setYear(Integer year) {
        this.year = year;
        return (T) this;
    }

    public @Nullable String getGenre() {
        return genre;
    }

    public T setGenre(String genre) {
        this.genre = genre;
        return (T) this;
    }
}
