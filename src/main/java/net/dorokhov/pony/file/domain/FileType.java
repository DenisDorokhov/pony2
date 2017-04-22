package net.dorokhov.pony.file.domain;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class FileType {

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
    public boolean equals(Object obj) {
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
        return "FileType{" +
                "mimeType='" + mimeType + '\'' +
                ", fileExtension='" + fileExtension + '\'' +
                '}';
    }
}
