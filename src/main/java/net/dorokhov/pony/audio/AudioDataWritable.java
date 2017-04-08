package net.dorokhov.pony.audio;

import com.google.common.base.MoreObjects;

import java.io.File;
import java.util.Optional;

public class AudioDataWritable extends AudioDataAbstract {

    private final boolean writeDiscNumber;
    private final boolean writeDiscCount;

    private final boolean writeTrackNumber;
    private final boolean writeTrackCount;

    private final boolean writeTitle;
    private final boolean writeArtist;
    private final boolean writeAlbumArtist;
    private final boolean writeAlbum;

    private final boolean writeYear;

    private final boolean writeGenre;

    private final boolean writeArtwork;
    private final File artworkFile;

    private AudioDataWritable(Builder builder) {
        super(builder);
        this.writeDiscNumber = builder.writeDiscNumber;
        this.writeDiscCount = builder.writeDiscCount;
        this.writeTrackNumber = builder.writeTrackNumber;
        this.writeTrackCount = builder.writeTrackCount;
        this.writeTitle = builder.writeTitle;
        this.writeArtist = builder.writeArtist;
        this.writeAlbumArtist = builder.writeAlbumArtist;
        this.writeAlbum = builder.writeAlbum;
        this.writeYear = builder.writeYear;
        this.writeGenre = builder.writeGenre;
        this.writeArtwork = builder.writeArtwork;
        this.artworkFile = builder.artworkFile;
    }

    public boolean isWriteDiscNumber() {
        return writeDiscNumber;
    }

    public boolean isWriteDiscCount() {
        return writeDiscCount;
    }

    public boolean isWriteTrackNumber() {
        return writeTrackNumber;
    }

    public boolean isWriteTrackCount() {
        return writeTrackCount;
    }

    public boolean isWriteTitle() {
        return writeTitle;
    }

    public boolean isWriteArtist() {
        return writeArtist;
    }

    public boolean isWriteAlbumArtist() {
        return writeAlbumArtist;
    }

    public boolean isWriteAlbum() {
        return writeAlbum;
    }

    public boolean isWriteYear() {
        return writeYear;
    }

    public boolean isWriteGenre() {
        return writeGenre;
    }

    public boolean isWriteArtwork() {
        return writeArtwork;
    }

    public Optional<File> getArtworkFile() {
        return Optional.ofNullable(artworkFile);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("writeDiscNumber", writeDiscNumber)
                .add("writeDiscCount", writeDiscCount)
                .add("writeTrackNumber", writeTrackNumber)
                .add("writeTrackCount", writeTrackCount)
                .add("writeTitle", writeTitle)
                .add("writeArtist", writeArtist)
                .add("writeAlbumArtist", writeAlbumArtist)
                .add("writeAlbum", writeAlbum)
                .add("writeYear", writeYear)
                .add("writeGenre", writeGenre)
                .add("writeArtwork", writeArtwork)
                .add("artworkFile", artworkFile)
                .add("discNumber", discNumber)
                .add("discCount", discCount)
                .add("trackNumber", trackNumber)
                .add("trackCount", trackCount)
                .add("title", title)
                .add("artist", artist)
                .add("albumArtist", albumArtist)
                .add("album", album)
                .add("year", year)
                .add("genre", genre)
                .toString();
    }

    public static class Builder extends BuilderAbstract {
        
        private boolean writeDiscNumber;
        private boolean writeDiscCount;
        
        private boolean writeTrackNumber;
        private boolean writeTrackCount;
        
        private boolean writeTitle;
        private boolean writeArtist;
        private boolean writeAlbumArtist;
        private boolean writeAlbum;
        private boolean writeYear;
        private boolean writeGenre;
        
        private boolean writeArtwork;
        private File artworkFile;

        @Override
        public Builder setDiscNumber(Integer discNumber) {
            super.setDiscNumber(discNumber);
            this.writeDiscNumber = true;
            return this;
        }

        @Override
        public Builder setDiscCount(Integer discCount) {
            super.setDiscCount(discCount);
            this.writeDiscCount = true;
            return this;
        }

        @Override
        public Builder setTrackNumber(Integer trackNumber) {
            super.setTrackNumber(trackNumber);
            this.writeTrackNumber = true;
            return this;
        }

        @Override
        public Builder setTrackCount(Integer trackCount) {
            super.setTrackCount(trackCount);
            this.writeTrackCount = true;
            return this;
        }

        @Override
        public Builder setTitle(String title) {
            super.setTitle(title);
            this.writeTitle = true;
            return this;
        }

        @Override
        public Builder setArtist(String artist) {
            super.setArtist(artist);
            this.writeArtist = true;
            return this;
        }

        @Override
        public Builder setAlbumArtist(String albumArtist) {
            super.setAlbumArtist(albumArtist);
            this.writeAlbumArtist = true;
            return this;
        }

        @Override
        public Builder setAlbum(String album) {
            super.setAlbum(album);
            this.writeAlbum = true;
            return this;
        }

        @Override
        public Builder setYear(Integer year) {
            super.setYear(year);
            this.writeYear = true;
            return this;
        }

        @Override
        public Builder setGenre(String genre) {
            super.setGenre(genre);
            this.writeGenre = true;
            return this;
        }

        public Builder setArtworkFile(File artworkFile) {
            this.artworkFile = artworkFile;
            this.writeArtwork = true;
            return this;
        }

        @Override
        public AudioDataWritable build() {
            return new AudioDataWritable(this);
        }
    }
}
