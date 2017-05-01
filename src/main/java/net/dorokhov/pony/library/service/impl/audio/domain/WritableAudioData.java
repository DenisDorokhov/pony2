package net.dorokhov.pony.library.service.impl.audio.domain;

import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;

public class WritableAudioData extends AudioData {

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

    public void ifShouldUpdateDiscNumber(Consumer<Integer> handler) {
        if (writeDiscNumber) {
            getDiscNumber().ifPresent(handler);
        }
    }

    public void ifShouldDeleteDiscNumber(Runnable handler) {
        if (writeDiscNumber && discNumber == null) {
            handler.run();
        }
    }

    public boolean shouldWriteDiscCount() {
        return writeDiscCount;
    }

    public void ifShouldUpdateDiscCount(Consumer<Integer> handler) {
        if (writeDiscCount) {
            getDiscCount().ifPresent(handler);
        }
    }

    public void ifShouldDeleteDiscCount(Runnable handler) {
        if (writeDiscCount && discCount == null) {
            handler.run();
        }
    }

    public boolean shouldWriteTrackNumber() {
        return writeTrackNumber;
    }

    public void ifShouldUpdateTrackNumber(Consumer<Integer> handler) {
        if (writeTrackNumber) {
            getTrackNumber().ifPresent(handler);
        }
    }

    public void ifShouldDeleteTrackNumber(Runnable handler) {
        if (writeTrackNumber && trackNumber == null) {
            handler.run();
        }
    }

    public boolean shouldWriteTrackCount() {
        return writeTrackCount;
    }

    public void ifShouldUpdateTrackCount(Consumer<Integer> handler) {
        if (writeTrackCount) {
            getTrackCount().ifPresent(handler);
        }
    }

    public void ifShouldDeleteTrackCount(Runnable handler) {
        if (writeTrackCount && trackCount == null) {
            handler.run();
        }
    }

    public boolean shouldWriteTitle() {
        return writeTitle;
    }

    public void ifShouldUpdateTitle(Consumer<String> handler) {
        if (writeTitle) {
            getTitle().ifPresent(handler);
        }
    }

    public void ifShouldDeleteTitle(Runnable handler) {
        if (writeTitle && title == null) {
            handler.run();
        }
    }

    public boolean shouldWriteArtist() {
        return writeArtist;
    }

    public void ifShouldUpdateArtist(Consumer<String> handler) {
        if (writeArtist) {
            getArtist().ifPresent(handler);
        }
    }

    public void ifShouldDeleteArtist(Runnable handler) {
        if (writeArtist && artist == null) {
            handler.run();
        }
    }

    public boolean shouldWriteAlbumArtist() {
        return writeAlbumArtist;
    }

    public void ifShouldUpdateAlbumArtist(Consumer<String> handler) {
        if (writeAlbumArtist) {
            getAlbumArtist().ifPresent(handler);
        }
    }

    public void ifShouldDeleteAlbumArtist(Runnable handler) {
        if (writeAlbumArtist && albumArtist == null) {
            handler.run();
        }
    }

    public boolean shouldWriteAlbum() {
        return writeAlbum;
    }

    public void ifShouldUpdateAlbum(Consumer<String> handler) {
        if (writeAlbum) {
            getAlbum().ifPresent(handler);
        }
    }

    public void ifShouldDeleteAlbum(Runnable handler) {
        if (writeAlbum && album == null) {
            handler.run();
        }
    }

    public boolean shouldWriteYear() {
        return writeYear;
    }

    public void ifShouldUpdateYear(Consumer<Integer> handler) {
        if (writeYear) {
            getYear().ifPresent(handler);
        }
    }

    public void ifShouldDeleteYear(Runnable handler) {
        if (writeYear && year == null) {
            handler.run();
        }
    }

    public boolean shouldWriteGenre() {
        return writeGenre;
    }

    public void ifShouldUpdateGenre(Consumer<String> handler) {
        if (writeGenre) {
            getGenre().ifPresent(handler);
        }
    }

    public void ifShouldDeleteGenre(Runnable handler) {
        if (writeGenre && genre == null) {
            handler.run();
        }
    }

    public boolean shouldWriteArtwork() {
        return writeArtwork;
    }

    public void ifShouldUpdateArtwork(Consumer<File> handler) {
        if (writeArtwork) {
            getArtworkFile().ifPresent(handler);
        }
    }

    public void ifShouldDeleteArtwork(Runnable handler) {
        if (writeArtwork && artworkFile == null) {
            handler.run();
        }
    }

    public Optional<File> getArtworkFile() {
        return Optional.ofNullable(artworkFile);
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

    public static class Builder extends BuilderAbstract<Builder> {

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
        public Builder discNumber(Integer discNumber) {
            super.discNumber(discNumber);
            this.writeDiscNumber = true;
            return this;
        }
        
        public Builder clearDiscNumber() {
            return discNumber(null);
        }

        @Override
        public Builder discCount(Integer discCount) {
            super.discCount(discCount);
            this.writeDiscCount = true;
            return this;
        }
        
        public Builder clearDiscCount() {
            return discCount(null);
        }

        @Override
        public Builder trackNumber(Integer trackNumber) {
            super.trackNumber(trackNumber);
            this.writeTrackNumber = true;
            return this;
        }
        
        public Builder clearTrackNumber() {
            return trackNumber(null);
        }

        @Override
        public Builder trackCount(Integer trackCount) {
            super.trackCount(trackCount);
            this.writeTrackCount = true;
            return this;
        }
        
        public Builder clearTrackCount() {
            return trackCount(null);
        }

        @Override
        public Builder title(String title) {
            super.title(title);
            this.writeTitle = true;
            return this;
        }
        
        public Builder clearTitle() {
            return title(null);
        }

        @Override
        public Builder artist(String artist) {
            super.artist(artist);
            this.writeArtist = true;
            return this;
        }
        
        public Builder clearArtist() {
            return artist(null);
        }

        @Override
        public Builder albumArtist(String albumArtist) {
            super.albumArtist(albumArtist);
            this.writeAlbumArtist = true;
            return this;
        }
        
        public Builder clearAlbumArtist() {
            return albumArtist(null);
        }

        @Override
        public Builder album(String album) {
            super.album(album);
            this.writeAlbum = true;
            return this;
        }
        
        public Builder clearAlbum() {
            return album(null);
        }

        @Override
        public Builder year(Integer year) {
            super.year(year);
            this.writeYear = true;
            return this;
        }
        
        public Builder clearYear() {
            return year(null);
        }

        @Override
        public Builder genre(String genre) {
            super.genre(genre);
            this.writeGenre = true;
            return this;
        }
        
        public Builder clearGenre() {
            return genre(null);
        }

        public Builder artworkFile(File artworkFile) {
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
