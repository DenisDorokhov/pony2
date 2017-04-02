package net.dorokhov.pony.audio;

import com.google.common.base.MoreObjects;
import com.google.common.io.ByteSource;
import net.dorokhov.pony.file.FileType;

import java.util.Optional;

public class AudioDataReadable extends AudioDataAbstract {

    private final String path;
    private final FileType fileType;
    private final long size;
    private final long duration;
    private final long bitRate;
    private final boolean bitRateVariable;
    private final EmbeddedArtwork embeddedArtwork;

    private AudioDataReadable(Integer discNumber, Integer discCount,
                              Integer trackNumber, Integer trackCount,
                              String title, String artist, String albumArtist, String album, Integer year, String genre,
                              String path, FileType fileType, long size, long duration, long bitRate, boolean bitRateVariable,
                              EmbeddedArtwork embeddedArtwork) {
        super(discNumber, discCount, trackNumber, trackCount, title, artist, albumArtist, album, year, genre);
        this.path = path;
        this.fileType = fileType;
        this.size = size;
        this.duration = duration;
        this.bitRate = bitRate;
        this.bitRateVariable = bitRateVariable;
        this.embeddedArtwork = embeddedArtwork;
    }

    public String getPath() {
        return path;
    }

    public FileType getFileType() {
        return fileType;
    }

    public long getSize() {
        return size;
    }

    public long getDuration() {
        return duration;
    }

    public long getBitRate() {
        return bitRate;
    }

    public boolean isBitRateVariable() {
        return bitRateVariable;
    }

    public Optional<EmbeddedArtwork> getEmbeddedArtwork() {
        return Optional.ofNullable(embeddedArtwork);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", path)
                .add("fileType", fileType)
                .add("size", size)
                .add("duration", duration)
                .add("bitRate", bitRate)
                .add("bitRateVariable", bitRateVariable)
                .add("embeddedArtwork", embeddedArtwork)
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

    public static class EmbeddedArtwork {

        private final ByteSource binaryData;

        private final FileType fileType;

        private final String checksum;

        public EmbeddedArtwork(ByteSource binaryData, FileType fileType, String checksum) {
            this.binaryData = binaryData;
            this.fileType = fileType;
            this.checksum = checksum;
        }

        public ByteSource getBinaryData() {
            return binaryData;
        }

        public FileType getFileType() {
            return fileType;
        }

        public String getChecksum() {
            return checksum;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("fileType", fileType)
                    .add("checksum", checksum)
                    .toString();
        }
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
        private String path;
        
        private FileType fileType;
        private long size;
        private long duration;
        private long bitRate;
        private boolean bitRateVariable;
        private EmbeddedArtwork embeddedArtwork;

        public Builder setDiscNumber(Integer discNumber) {
            this.discNumber = discNumber;
            return this;
        }

        public Builder setDiscCount(Integer discCount) {
            this.discCount = discCount;
            return this;
        }

        public Builder setTrackNumber(Integer trackNumber) {
            this.trackNumber = trackNumber;
            return this;
        }

        public Builder setTrackCount(Integer trackCount) {
            this.trackCount = trackCount;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setArtist(String artist) {
            this.artist = artist;
            return this;
        }

        public Builder setAlbumArtist(String albumArtist) {
            this.albumArtist = albumArtist;
            return this;
        }

        public Builder setAlbum(String album) {
            this.album = album;
            return this;
        }

        public Builder setYear(Integer year) {
            this.year = year;
            return this;
        }

        public Builder setGenre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setFileType(FileType fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder setSize(long size) {
            this.size = size;
            return this;
        }

        public Builder setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder setBitRate(long bitRate) {
            this.bitRate = bitRate;
            return this;
        }

        public Builder setBitRateVariable(boolean bitRateVariable) {
            this.bitRateVariable = bitRateVariable;
            return this;
        }

        public Builder setEmbeddedArtwork(EmbeddedArtwork embeddedArtwork) {
            this.embeddedArtwork = embeddedArtwork;
            return this;
        }

        public AudioDataReadable build() {
            return new AudioDataReadable(discNumber, discCount, 
                    trackNumber, trackCount, 
                    title, artist, albumArtist, album, year, genre, 
                    path, fileType, size, duration, bitRate, bitRateVariable,
                    embeddedArtwork);
        }
    }
}
