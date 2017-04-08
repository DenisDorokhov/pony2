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

    private AudioDataReadable(Builder builder) {
        super(builder);
        this.path = builder.path;
        this.fileType = builder.fileType;
        this.size = builder.size;
        this.duration = builder.duration;
        this.bitRate = builder.bitRate;
        this.bitRateVariable = builder.bitRateVariable;
        this.embeddedArtwork = builder.embeddedArtwork;
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
    
    public static class Builder extends BuilderAbstract {

        private String path;
        private FileType fileType;
        private long size;
        private long duration;
        private long bitRate;
        private boolean bitRateVariable;
        private EmbeddedArtwork embeddedArtwork;

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

        @Override
        public AudioDataReadable build() {
            return new AudioDataReadable(this);
        }
    }
}
