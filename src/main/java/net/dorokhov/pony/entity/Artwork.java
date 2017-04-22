package net.dorokhov.pony.entity;

import com.google.common.collect.ImmutableMap;
import net.dorokhov.pony.util.JsonAttributeConverter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "artwork", uniqueConstraints = @UniqueConstraint(columnNames = {"tag", "checksum"}))
public class Artwork implements Identity<Long>, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "date", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime date;

    @Column(name = "mime_type", nullable = false)
    @NotNull
    private String mimeType;

    @Column(name = "checksum", nullable = false)
    @NotNull
    private String checksum;

    @Column(name = "large_image_size", nullable = false)
    @NotNull
    private long largeImageSize;

    @Column(name = "large_image_path", nullable = false, unique = true)
    @NotNull
    private String largeImagePath;

    @Column(name = "small_image_size", nullable = false)
    @NotNull
    private long smallImageSize;

    @Column(name = "small_image_path", nullable = false, unique = true)
    @NotNull
    private String smallImagePath;

    @Column(name = "tag")
    private String tag;

    @Column(name = "meta_data")
    @Convert(converter = JsonAttributeConverter.MapConverter.class)
    private Map<String, String> metaData = ImmutableMap.of();

    protected Artwork() {
    }

    private Artwork(Builder builder) {
        id = builder.id;
        date = builder.date;
        mimeType = checkNotNull(builder.mimeType);
        checksum = checkNotNull(builder.checksum);
        largeImageSize = checkNotNull(builder.largeImageSize);
        largeImagePath = checkNotNull(builder.largeImagePath);
        smallImageSize = checkNotNull(builder.smallImageSize);
        smallImagePath = checkNotNull(builder.smallImagePath);
        tag = builder.tag;
        metaData = builder.metaData.build();
    }

    @Override
    public Long getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getChecksum() {
        return checksum;
    }

    public long getLargeImageSize() {
        return largeImageSize;
    }

    public String getLargeImagePath() {
        return largeImagePath;
    }

    public long getSmallImageSize() {
        return smallImageSize;
    }

    public String getSmallImagePath() {
        return smallImagePath;
    }

    public Optional<String> getTag() {
        return Optional.ofNullable(tag);
    }

    public Map<String, String> getMetaData() {
        return metaData != null ? metaData : ImmutableMap.of();
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
        return "Artwork{" +
                "id=" + id +
                ", mimeType='" + mimeType + '\'' +
                ", checksum='" + checksum + '\'' +
                ", largeImagePath='" + largeImagePath + '\'' +
                ", smallImagePath='" + smallImagePath + '\'' +
                ", tag='" + tag + '\'' +
                ", metaData=" + metaData +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(Artwork artwork) {
        return new Builder(artwork);
    }

    public static class Builder {
        
        private Long id;
        private LocalDateTime date;
        private String mimeType;
        private String checksum;
        private Long largeImageSize;
        private String largeImagePath;
        private Long smallImageSize;
        private String smallImagePath;
        private String tag;
        private ImmutableMap.Builder<String, String> metaData = ImmutableMap.builder();

        public Builder() {
        }
        
        public Builder(Artwork artwork) {
            id = artwork.id;
            date = artwork.date;
            mimeType = artwork.mimeType;
            checksum = artwork.checksum;
            largeImageSize = artwork.largeImageSize;
            largeImagePath = artwork.largeImagePath;
            smallImageSize = artwork.smallImageSize;
            smallImagePath = artwork.smallImagePath;
            tag = artwork.tag;
            metaData = ImmutableMap.<String, String>builder().putAll(artwork.metaData);
        }

        Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder date(LocalDateTime date) {
            this.date = date;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder checksum(String checksum) {
            this.checksum = checksum;
            return this;
        }

        public Builder largeImageSize(Long val) {
            largeImageSize = val;
            return this;
        }

        public Builder largeImagePath(String largeImagePath) {
            this.largeImagePath = largeImagePath;
            return this;
        }

        public Builder smallImageSize(Long smallImageSize) {
            this.smallImageSize = smallImageSize;
            return this;
        }

        public Builder smallImagePath(String smallImagePath) {
            this.smallImagePath = smallImagePath;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder metaData(Map<String, String> metaData) {
            if (metaData != null) {
                this.metaData = ImmutableMap.<String, String>builder().putAll(metaData);
            } else {
                this.metaData = ImmutableMap.builder();
            }
            return this;
        }

        public Builder putMetaData(String key, String value) {
            metaData.put(key, value);
            return this;
        }

        public Artwork build() {
            return new Artwork(this);
        }
    }
}
