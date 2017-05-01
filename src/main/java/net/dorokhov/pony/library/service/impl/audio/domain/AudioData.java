package net.dorokhov.pony.library.service.impl.audio.domain;

import javax.annotation.Nullable;

abstract class AudioData {

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
    public String getTitle() {
        return title;
    }
    
    @Nullable
    public String getArtist() {
        return artist;
    }

    @Nullable
    public String getAlbumArtist() {
        return albumArtist;
    }

    @Nullable
    public String getAlbum() {
        return album;
    }

    @Nullable
    public Integer getYear() {
        return year;
    }

    @Nullable
    public String getGenre() {
        return genre;
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

        public T discNumber(@Nullable Integer discNumber) {
            this.discNumber = discNumber;
            return self();
        }

        public T discCount(@Nullable Integer discCount) {
            this.discCount = discCount;
            return self();
        }

        public T trackNumber(@Nullable Integer trackNumber) {
            this.trackNumber = trackNumber;
            return self();
        }

        public T trackCount(@Nullable Integer trackCount) {
            this.trackCount = trackCount;
            return self();
        }

        public T title(@Nullable String title) {
            this.title = title;
            return self();
        }

        public T artist(@Nullable String artist) {
            this.artist = artist;
            return self();
        }

        public T albumArtist(@Nullable String albumArtist) {
            this.albumArtist = albumArtist;
            return self();
        }

        public T album(@Nullable String album) {
            this.album = album;
            return self();
        }

        public T year(@Nullable Integer year) {
            this.year = year;
            return self();
        }

        public T genre(@Nullable String genre) {
            this.genre = genre;
            return self();
        }
        
        abstract protected T self();

        abstract public AudioData build();
    }
}
