package net.dorokhov.pony.library.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.net.URI;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "artwork")
public class Artwork implements Serializable {

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

    @Column(name = "checksum", nullable = false, unique = true)
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

    @Column(name = "source_uri")
    private URI sourceUri;

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
        sourceUri = checkNotNull(builder.sourceUri);
    }

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

    public URI getSourceUri() {
        return sourceUri;
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
    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    public boolean equals(@Nullable Object obj) {
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
                ", sourceUri=" + sourceUri +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private Long id;
        private LocalDateTime date;
        private String mimeType;
        private String checksum;
        private Long largeImageSize;
        private String largeImagePath;
        private Long smallImageSize;
        private String smallImagePath;
        private URI sourceUri;

        public Builder() {
        }

        public Builder id(@Nullable Long id) {
            this.id = id;
            return this;
        }

        public Builder date(@Nullable LocalDateTime date) {
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

        public Builder sourceUri(URI sourceFilePath) {
            this.sourceUri = sourceFilePath;
            return this;
        }

        public Artwork build() {
            return new Artwork(this);
        }
    }
}
