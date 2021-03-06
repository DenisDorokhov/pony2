package net.dorokhov.pony.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.io.ByteSource;

import javax.annotation.Nullable;
import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ReadableAudioData extends AbstractAudioData {

    private final String path;
    private final FileType fileType;
    private final long size;
    private final long duration;
    private final long bitRate;
    private final boolean bitRateVariable;
    private final EmbeddedArtwork embeddedArtwork;

    private ReadableAudioData(Builder builder) {
        super(builder);
        this.path = checkNotNull(builder.path);
        this.fileType = checkNotNull(builder.fileType);
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

    @Nullable
    public EmbeddedArtwork getEmbeddedArtwork() {
        return embeddedArtwork;
    }
    
    public File getFile() {
        return new File(path);
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class EmbeddedArtwork {

        private final ByteSource binaryData;

        private final FileType fileType;

        public EmbeddedArtwork(ByteSource binaryData, FileType fileType) {
            this.binaryData = checkNotNull(binaryData);
            this.fileType = checkNotNull(fileType);
        }

        public ByteSource getBinaryData() {
            return binaryData;
        }

        public FileType getFileType() {
            return fileType;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("fileType", fileType)
                    .toString();
        }
    }
    
    public static final class Builder extends AbstractBuilder<Builder> {

        private String path;
        private FileType fileType;
        private long size;
        private long duration;
        private long bitRate;
        private boolean bitRateVariable;
        private EmbeddedArtwork embeddedArtwork;

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder fileType(FileType fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder bitRate(long bitRate) {
            this.bitRate = bitRate;
            return this;
        }

        public Builder bitRateVariable(boolean bitRateVariable) {
            this.bitRateVariable = bitRateVariable;
            return this;
        }

        public Builder embeddedArtwork(@Nullable EmbeddedArtwork embeddedArtwork) {
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
