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

    private AudioDataWritable(Integer discNumber, Integer discCount, 
                             Integer trackNumber, Integer trackCount, 
                             String title, String artist, String albumArtist, String album, Integer year, String genre, 
                             boolean writeDiscNumber, boolean writeDiscCount, 
                             boolean writeTrackNumber, boolean writeTrackCount, 
                             boolean writeTitle, boolean writeArtist, boolean writeAlbumArtist, boolean writeAlbum, 
                             boolean writeYear, boolean writeGenre, 
                             boolean writeArtwork, File artworkFile) {
        super(discNumber, discCount, trackNumber, trackCount, title, artist, albumArtist, album, year, genre);
        this.writeDiscNumber = writeDiscNumber;
        this.writeDiscCount = writeDiscCount;
        this.writeTrackNumber = writeTrackNumber;
        this.writeTrackCount = writeTrackCount;
        this.writeTitle = writeTitle;
        this.writeArtist = writeArtist;
        this.writeAlbumArtist = writeAlbumArtist;
        this.writeAlbum = writeAlbum;
        this.writeYear = writeYear;
        this.writeGenre = writeGenre;
        this.writeArtwork = writeArtwork;
        this.artworkFile = artworkFile;
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

    public static class Builder {

        private Integer discNumber;
        private Integer discCount;
        
        private Integer trackNumber;
        private Integer trackCount;
        
        private String title;
        private String artist;
        private String albumArtist;
        private String album;
        private Integer year;
        private String genre;
        
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

        public Builder setDiscNumber(Integer discNumber) {
            this.discNumber = discNumber;
            this.writeDiscNumber = true;
            return this;
        }

        public Builder setDiscCount(Integer discCount) {
            this.discCount = discCount;
            this.writeDiscCount = true;
            return this;
        }

        public Builder setTrackNumber(Integer trackNumber) {
            this.trackNumber = trackNumber;
            this.writeTrackNumber = true;
            return this;
        }

        public Builder setTrackCount(Integer trackCount) {
            this.trackCount = trackCount;
            this.writeTrackCount = true;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            this.writeTitle = true;
            return this;
        }

        public Builder setArtist(String artist) {
            this.artist = artist;
            this.writeArtist = true;
            return this;
        }

        public Builder setAlbumArtist(String albumArtist) {
            this.albumArtist = albumArtist;
            this.writeAlbumArtist = true;
            return this;
        }

        public Builder setAlbum(String album) {
            this.album = album;
            this.writeAlbum = true;
            return this;
        }

        public Builder setYear(Integer year) {
            this.year = year;
            this.writeYear = true;
            return this;
        }

        public Builder setGenre(String genre) {
            this.genre = genre;
            this.writeGenre = true;
            return this;
        }

        public Builder setArtworkFile(File artworkFile) {
            this.artworkFile = artworkFile;
            this.writeArtwork = true;
            return this;
        }

        public AudioDataWritable build() {
            return new AudioDataWritable(discNumber, discCount, trackNumber, trackCount, title, artist, albumArtist, album, year, genre, writeDiscNumber, writeDiscCount, writeTrackNumber, writeTrackCount, writeTitle, writeArtist, writeAlbumArtist, writeAlbum, writeYear, writeGenre, writeArtwork, artworkFile);
        }
    }
}
