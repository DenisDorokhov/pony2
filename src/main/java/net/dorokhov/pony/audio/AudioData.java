package net.dorokhov.pony.audio;

import java.util.Optional;

public abstract class AudioData {

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

    protected AudioData(BuilderAbstract builder) {
        this.discNumber = builder.discNumber;
        this.discCount = builder.discCount;
        this.trackNumber = builder.trackNumber;
        this.trackCount = builder.trackCount;
        this.title = builder.title;
        this.artist = builder.artist;
        this.albumArtist = builder.albumArtist;
        this.album = builder.album;
        this.year = builder.year;
        this.genre = builder.genre;
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
    
    protected abstract static class BuilderAbstract<T extends BuilderAbstract> {

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

        public T setDiscNumber(final Integer discNumber) {
            this.discNumber = discNumber;
            return self();
        }

        public T setDiscCount(final Integer discCount) {
            this.discCount = discCount;
            return self();
        }

        public T setTrackNumber(final Integer trackNumber) {
            this.trackNumber = trackNumber;
            return self();
        }

        public T setTrackCount(final Integer trackCount) {
            this.trackCount = trackCount;
            return self();
        }

        public T setTitle(final String title) {
            this.title = title;
            return self();
        }

        public T setArtist(final String artist) {
            this.artist = artist;
            return self();
        }

        public T setAlbumArtist(final String albumArtist) {
            this.albumArtist = albumArtist;
            return self();
        }

        public T setAlbum(final String album) {
            this.album = album;
            return self();
        }

        public T setYear(final Integer year) {
            this.year = year;
            return self();
        }

        public T setGenre(final String genre) {
            this.genre = genre;
            return self();
        }
        
        abstract public T self();

        abstract public AudioData build();
    }
}
