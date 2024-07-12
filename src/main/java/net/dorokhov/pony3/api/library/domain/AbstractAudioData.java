package net.dorokhov.pony3.api.library.domain;

import jakarta.annotation.Nullable;

abstract class AbstractAudioData {

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

    @Nullable
    public Integer getDiscNumber() {
        return discNumber;
    }

    public AbstractAudioData setDiscNumber(@Nullable Integer discNumber) {
        this.discNumber = discNumber;
        return this;
    }

    @Nullable
    public Integer getDiscCount() {
        return discCount;
    }

    public AbstractAudioData setDiscCount(Integer discCount) {
        this.discCount = discCount;
        return this;
    }

    @Nullable
    public Integer getTrackNumber() {
        return trackNumber;
    }

    public AbstractAudioData setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
        return this;
    }

    @Nullable
    public Integer getTrackCount() {
        return trackCount;
    }

    public AbstractAudioData setTrackCount(Integer trackCount) {
        this.trackCount = trackCount;
        return this;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public AbstractAudioData setTitle(String title) {
        this.title = title;
        return this;
    }

    @Nullable
    public String getArtist() {
        return artist;
    }

    public AbstractAudioData setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    @Nullable
    public String getAlbumArtist() {
        return albumArtist;
    }

    public AbstractAudioData setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
        return this;
    }

    @Nullable
    public String getAlbum() {
        return album;
    }

    public AbstractAudioData setAlbum(String album) {
        this.album = album;
        return this;
    }

    @Nullable
    public Integer getYear() {
        return year;
    }

    public AbstractAudioData setYear(Integer year) {
        this.year = year;
        return this;
    }

    @Nullable
    public String getGenre() {
        return genre;
    }

    public AbstractAudioData setGenre(String genre) {
        this.genre = genre;
        return this;
    }
}
