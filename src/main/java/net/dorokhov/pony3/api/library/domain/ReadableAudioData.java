package net.dorokhov.pony3.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.io.ByteSource;
import jakarta.annotation.Nullable;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ReadableAudioData extends AbstractAudioData<ReadableAudioData> {

    private String path;
    private FileType fileType;
    private long size;
    private long duration;
    private long bitRate;
    private boolean bitRateVariable;
    private EmbeddedArtwork embeddedArtwork;

    public String getPath() {
        return path;
    }

    public ReadableAudioData setPath(String path) {
        this.path = path;
        return this;
    }

    public FileType getFileType() {
        return fileType;
    }

    public ReadableAudioData setFileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    public long getSize() {
        return size;
    }

    public ReadableAudioData setSize(long size) {
        this.size = size;
        return this;
    }

    public long getDuration() {
        return duration;
    }

    public ReadableAudioData setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public long getBitRate() {
        return bitRate;
    }

    public ReadableAudioData setBitRate(long bitRate) {
        this.bitRate = bitRate;
        return this;
    }

    public boolean isBitRateVariable() {
        return bitRateVariable;
    }

    public ReadableAudioData setBitRateVariable(boolean bitRateVariable) {
        this.bitRateVariable = bitRateVariable;
        return this;
    }

    public @Nullable EmbeddedArtwork getEmbeddedArtwork() {
        return embeddedArtwork;
    }

    public ReadableAudioData setEmbeddedArtwork(@Nullable EmbeddedArtwork embeddedArtwork) {
        this.embeddedArtwork = embeddedArtwork;
        return this;
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
}
