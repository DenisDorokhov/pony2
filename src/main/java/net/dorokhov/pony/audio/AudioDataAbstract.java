package net.dorokhov.pony.audio;

import java.util.Optional;

public abstract class AudioDataAbstract {

    protected final Integer discNumber;
    protected final Integer discCount;
    protected final Integer trackNumber;
    protected final Integer trackCount;
    protected final String title;
    protected final String artist;
    protected final String albumArtist;
    protected final String album;
    protected final Integer year;
    protected final String genre;

    protected AudioDataAbstract(Integer discNumber, Integer discCount, Integer trackNumber, Integer trackCount, String title, String artist, String albumArtist, String album, Integer year, String genre) {
        this.discNumber = discNumber;
        this.discCount = discCount;
        this.trackNumber = trackNumber;
        this.trackCount = trackCount;
        this.title = title;
        this.artist = artist;
        this.albumArtist = albumArtist;
        this.album = album;
        this.year = year;
        this.genre = genre;
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

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public Optional<String> getArtist() {
        return Optional.ofNullable(artist);
    }

    public Optional<String> getAlbumArtist() {
        return Optional.ofNullable(albumArtist);
    }

    public Optional<String> getAlbum() {
        return Optional.ofNullable(album);
    }

    public Optional<Integer> getYear() {
        return Optional.ofNullable(year);
    }

    public Optional<String> getGenre() {
        return Optional.ofNullable(genre);
    }
}
