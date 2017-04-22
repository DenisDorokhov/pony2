package net.dorokhov.pony.audio;

import com.google.common.io.ByteSource;
import net.dorokhov.pony.file.FileType;

import java.util.Optional;

public class ReadableAudioData extends AudioData {

    private final String path;
    private final FileType fileType;
    private final long size;
    private final long duration;
    private final long bitRate;
    private final boolean bitRateVariable;
    private final EmbeddedArtwork embeddedArtwork;

    private ReadableAudioData(Builder builder) {
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
        return "ReadableAudioData{" +
                "path='" + path + '\'' +
                ", fileType=" + fileType +
                ", size=" + size +
                ", duration=" + duration +
                ", bitRate=" + bitRate +
                ", bitRateVariable=" + bitRateVariable +
                ", embeddedArtwork=" + embeddedArtwork +
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
            return "EmbeddedArtwork{" +
                    "fileType=" + fileType +
                    ", checksum='" + checksum + '\'' +
                    '}';
        }
    }
    
    public static class Builder extends BuilderAbstract<Builder> {

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
        public Builder self() {
            return this;
        }

        @Override
        public ReadableAudioData build() {
            return new ReadableAudioData(this);
        }
    }
}
