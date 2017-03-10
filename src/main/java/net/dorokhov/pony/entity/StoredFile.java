package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import net.dorokhov.pony.util.JsonAttributeConverter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Entity
@Table(name = "stored_file", uniqueConstraints = @UniqueConstraint(columnNames = {"tag", "checksum"}))
public class StoredFile {

    public static final String TAG_ARTWORK_EMBEDDED = "artworkEmbedded";
    public static final String TAG_ARTWORK_FILE = "artworkFile";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "checksum", nullable = false)
    private String checksum;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "path", nullable = false, unique = true)
    private String path;

    @Column(name = "tag")
    private String tag;

    @Column(name = "user_data")
    @Convert(converter = JsonAttributeConverter.MapConverter.class)
    private Map<String, Object> metaData = new HashMap<>();

    private StoredFile() {
    }

    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    private void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Preconditions.checkNotNull(name);
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = Preconditions.checkNotNull(mimeType);
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = Preconditions.checkNotNull(checksum);
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = Preconditions.checkNotNull(size);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = Preconditions.checkNotNull(path);
    }

    public Optional<String> getTag() {
        return Optional.ofNullable(tag);
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = Preconditions.checkNotNull(metaData);
    }

    @PrePersist
    public void prePersist() {
        date = LocalDateTime.now();
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && id != null && getClass().equals(obj.getClass())) {
            StoredFile that = (StoredFile) obj;
            return id.equals(that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("date", date)
                .add("name", name)
                .add("mimeType", mimeType)
                .add("checksum", checksum)
                .add("size", size)
                .add("path", path)
                .add("tag", tag)
                .add("metaData", metaData)
                .toString();
    }

    public static class Builder {
        
        private Long id;
        private LocalDateTime date;
        private String name;
        private String mimeType;
        private String checksum;
        private Long size;
        private String path;
        private String tag;
        private Map<String, Object> metaData = new HashMap<>();

        public Builder() {
        }

        public Builder(StoredFile storedFile) {
            this.id = storedFile.id;
            this.date = storedFile.date;
            this.name = storedFile.name;
            this.mimeType = storedFile.mimeType;
            this.checksum = storedFile.checksum;
            this.size = storedFile.size;
            this.path = storedFile.path;
            this.tag = storedFile.tag;
            this.metaData = storedFile.metaData;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder setChecksum(String checksum) {
            this.checksum = checksum;
            return this;
        }

        public Builder setSize(Long size) {
            this.size = size;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setMetaData(Map<String, Object> metaData) {
            this.metaData = metaData;
            return this;
        }

        public StoredFile build() {
            StoredFile storedFile = new StoredFile();
            storedFile.setId(id);
            storedFile.setDate(date);
            storedFile.setName(name);
            storedFile.setMimeType(mimeType);
            storedFile.setChecksum(checksum);
            storedFile.setSize(size);
            storedFile.setPath(path);
            storedFile.setTag(tag);
            storedFile.setMetaData(metaData);
            return storedFile;
        }
    }
}
