package net.dorokhov.pony.audio;

import com.google.common.base.MoreObjects;

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
        
        public Builder unsetDiscNumber() {
            return setDiscNumber(null);
        }

        @Override
        public Builder setDiscCount(Integer discCount) {
            super.setDiscCount(discCount);
            this.writeDiscCount = true;
            return this;
        }
        
        public Builder unsetDiscCount() {
            return setDiscCount(null);
        }

        @Override
        public Builder setTrackNumber(Integer trackNumber) {
            super.setTrackNumber(trackNumber);
            this.writeTrackNumber = true;
            return this;
        }
        
        public Builder unsetTrackNumber() {
            return setTrackNumber(null);
        }

        @Override
        public Builder setTrackCount(Integer trackCount) {
            super.setTrackCount(trackCount);
            this.writeTrackCount = true;
            return this;
        }
        
        public Builder unsetTrackCount() {
            return setTrackCount(null);
        }

        @Override
        public Builder setTitle(String title) {
            super.setTitle(title);
            this.writeTitle = true;
            return this;
        }
        
        public Builder unsetTitle() {
            return setTitle(null);
        }

        @Override
        public Builder setArtist(String artist) {
            super.setArtist(artist);
            this.writeArtist = true;
            return this;
        }
        
        public Builder unsetArtist() {
            return setArtist(null);
        }

        @Override
        public Builder setAlbumArtist(String albumArtist) {
            super.setAlbumArtist(albumArtist);
            this.writeAlbumArtist = true;
            return this;
        }
        
        public Builder unsetAlbumArtist() {
            return setAlbumArtist(null);
        }

        @Override
        public Builder setAlbum(String album) {
            super.setAlbum(album);
            this.writeAlbum = true;
            return this;
        }
        
        public Builder unsetAlbum() {
            return setAlbum(null);
        }

        @Override
        public Builder setYear(Integer year) {
            super.setYear(year);
            this.writeYear = true;
            return this;
        }
        
        public Builder unsetYear() {
            return setYear(null);
        }

        @Override
        public Builder setGenre(String genre) {
            super.setGenre(genre);
            this.writeGenre = true;
            return this;
        }
        
        public Builder unsetGenre() {
            return setGenre(null);
        }

        public Builder setArtworkFile(File artworkFile) {
            this.artworkFile = artworkFile;
            this.writeArtwork = true;
            return this;
        }

        public Builder unsetArtworkFile() {
            return setArtworkFile(null);
        }

        @Override
        public WritableAudioData build() {
            return new WritableAudioData(this);
        }
    }
}
