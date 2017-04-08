package net.dorokhov.pony.file;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class FileType {

    private final String mimeType;

    private final String fileExtension;

    public FileType(String mimeType, String fileExtension) {
        Preconditions.checkNotNull(mimeType);
        Preconditions.checkNotNull(fileExtension);
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
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
