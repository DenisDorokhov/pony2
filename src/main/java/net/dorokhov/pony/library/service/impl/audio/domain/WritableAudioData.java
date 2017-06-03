package net.dorokhov.pony.library.service.impl.audio.domain;

import javax.annotation.Nullable;
import java.io.File;

public final class WritableAudioData extends AbstractAudioData {

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

    private WritableAudioData(Builder builder) {
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

    public boolean shouldWriteDiscNumber() {
        return writeDiscNumber;
    }

    public boolean shouldWriteDiscCount() {
        return writeDiscCount;
    }

    public boolean shouldWriteTrackNumber() {
        return writeTrackNumber;
    }

    public boolean shouldWriteTrackCount() {
        return writeTrackCount;
    }

    public boolean shouldWriteTitle() {
        return writeTitle;
    }

    public boolean shouldWriteArtist() {
        return writeArtist;
    }

    public boolean shouldWriteAlbumArtist() {
        return writeAlbumArtist;
    }

    public boolean shouldWriteAlbum() {
        return writeAlbum;
    }

    public boolean shouldWriteYear() {
        return writeYear;
    }

    public boolean shouldWriteGenre() {
        return writeGenre;
    }

    public boolean shouldWriteArtwork() {
        return writeArtwork;
    }

    @Nullable
    public File getArtworkFile() {
        return artworkFile;
    }

    @Override
    public String toString() {
        return "WritableAudioData{" +
                "writeDiscNumber=" + writeDiscNumber +
                ", writeDiscCount=" + writeDiscCount +
                ", writeTrackNumber=" + writeTrackNumber +
                ", writeTrackCount=" + writeTrackCount +
                ", writeTitle=" + writeTitle +
                ", writeArtist=" + writeArtist +
                ", writeAlbumArtist=" + writeAlbumArtist +
                ", writeAlbum=" + writeAlbum +
                ", writeYear=" + writeYear +
                ", writeGenre=" + writeGenre +
                ", writeArtwork=" + writeArtwork +
                ", artworkFile=" + artworkFile +
                ", discNumber=" + discNumber +
                ", discCount=" + discCount +
                ", trackNumber=" + trackNumber +
                ", trackCount=" + trackCount +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", albumArtist='" + albumArtist + '\'' +
                ", album='" + album + '\'' +
                ", year=" + year +
                ", genre='" + genre + '\'' +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractBuilder<Builder> {

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
        public Builder discNumber(@Nullable Integer discNumber) {
            super.discNumber(discNumber);
            this.writeDiscNumber = true;
            return this;
        }
        
        public Builder clearDiscNumber() {
            return discNumber(null);
        }

        @Override
        public Builder discCount(@Nullable Integer discCount) {
            super.discCount(discCount);
            this.writeDiscCount = true;
            return this;
        }
        
        public Builder clearDiscCount() {
            return discCount(null);
        }

        @Override
        public Builder trackNumber(@Nullable Integer trackNumber) {
            super.trackNumber(trackNumber);
            this.writeTrackNumber = true;
            return this;
        }
        
        public Builder clearTrackNumber() {
            return trackNumber(null);
        }

        @Override
        public Builder trackCount(@Nullable Integer trackCount) {
            super.trackCount(trackCount);
            this.writeTrackCount = true;
            return this;
        }
        
        public Builder clearTrackCount() {
            return trackCount(null);
        }

        @Override
        public Builder title(@Nullable String title) {
            super.title(title);
            this.writeTitle = true;
            return this;
        }
        
        public Builder clearTitle() {
            return title(null);
        }

        @Override
        public Builder artist(@Nullable String artist) {
            super.artist(artist);
            this.writeArtist = true;
            return this;
        }
        
        public Builder clearArtist() {
            return artist(null);
        }

        @Override
        public Builder albumArtist(@Nullable String albumArtist) {
            super.albumArtist(albumArtist);
            this.writeAlbumArtist = true;
            return this;
        }
        
        public Builder clearAlbumArtist() {
            return albumArtist(null);
        }

        @Override
        public Builder album(@Nullable String album) {
            super.album(album);
            this.writeAlbum = true;
            return this;
        }
        
        public Builder clearAlbum() {
            return album(null);
        }

        @Override
        public Builder year(@Nullable Integer year) {
            super.year(year);
            this.writeYear = true;
            return this;
        }
        
        public Builder clearYear() {
            return year(null);
        }

        @Override
        public Builder genre(@Nullable String genre) {
            super.genre(genre);
            this.writeGenre = true;
            return this;
        }
        
        public Builder clearGenre() {
            return genre(null);
        }

        public Builder artworkFile(@Nullable File artworkFile) {
            this.artworkFile = artworkFile;
            this.writeArtwork = true;
            return this;
        }

        public Builder clearArtworkFile() {
            return artworkFile(null);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public WritableAudioData build() {
            return new WritableAudioData(this);
        }
    }
}
