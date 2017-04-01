package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import net.dorokhov.pony.util.JsonAttributeConverter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Entity
@Table(name = "artwork", uniqueConstraints = @UniqueConstraint(columnNames = {"tag", "checksum"}))
public class Artwork implements Identifiable<Long> {

    public static final String TAG_ARTWORK_EMBEDDED = "artworkEmbedded";
    public static final String TAG_ARTWORK_FILE = "artworkFile";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "date", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime date;

    @Column(name = "name", nullable = false)
    @NotNull
    private String name;

    @Column(name = "mime_type", nullable = false)
    @NotNull
    private String mimeType;

    @Column(name = "checksum", nullable = false)
    @NotNull
    private String checksum;

    @Column(name = "large_image_size", nullable = false)
    @NotNull
    private Long largeImageSize;

    @Column(name = "large_image_path", nullable = false, unique = true)
    @NotNull
    private String largeImagePath;

    @Column(name = "small_image_size", nullable = false)
    @NotNull
    private Long smallImageSize;

    @Column(name = "small_image_path", nullable = false, unique = true)
    @NotNull
    private String smallImagePath;

    @Column(name = "tag")
    private String tag;

    @Column(name = "user_data")
    @Convert(converter = JsonAttributeConverter.MapConverter.class)
    private Map<String, Object> metaData;

    @Override
    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Long getLargeImageSize() {
        return largeImageSize;
    }

    public void setLargeImageSize(Long size) {
        this.largeImageSize = size;
    }

    public String getLargeImagePath() {
        return largeImagePath;
    }

    public void setLargeImagePath(String path) {
        this.largeImagePath = path;
    }

    public Long getSmallImageSize() {
        return smallImageSize;
    }

    public void setSmallImageSize(Long smallImageSize) {
        this.smallImageSize = smallImageSize;
    }

    public String getSmallImagePath() {
        return smallImagePath;
    }

    public void setSmallImagePath(String smallImagePath) {
        this.smallImagePath = smallImagePath;
    }

    public Optional<String> getTag() {
        return Optional.ofNullable(tag);
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Map<String, Object> getMetaData() {
        if (metaData == null) {
            metaData = new HashMap<>();
        }
        return metaData;
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
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
            Artwork that = (Artwork) obj;
            return id.equals(that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("path", largeImagePath)
                .add("tag", tag)
                .add("metaData", metaData)
                .toString();
    }
}
