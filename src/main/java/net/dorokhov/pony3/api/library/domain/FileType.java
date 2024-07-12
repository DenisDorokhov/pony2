package net.dorokhov.pony3.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FileType implements Serializable {

    private final String mimeType;
    private final String fileExtension;

    public FileType(String mimeType, String fileExtension) {
        this.mimeType = checkNotNull(mimeType);
        this.fileExtension = checkNotNull(fileExtension);
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public boolean isImage() {
        return mimeType.startsWith("image/");
    }

    public boolean isAudio() {
        return mimeType.startsWith("audio/");
    }

    @Override
    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FileType that = (FileType) obj;
        return Objects.equal(this.mimeType, that.mimeType) &&
                Objects.equal(this.fileExtension, that.fileExtension);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mimeType, fileExtension);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mimeType", mimeType)
                .add("fileExtension", fileExtension)
                .toString();
    }

    public static FileType of(String mimeType, String fileExtension) {
        return new FileType(mimeType, fileExtension);
    }
}
