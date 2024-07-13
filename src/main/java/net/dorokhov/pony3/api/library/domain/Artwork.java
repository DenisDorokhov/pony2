package net.dorokhov.pony3.api.library.domain;

import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.net.URI;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "artwork")
public class Artwork implements Serializable {
    
    public static final String SOURCE_URI_SCHEME_FILE = "file";
    public static final String SOURCE_URI_SCHEME_EMBEDDED = "embedded";

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private String id;

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
    @NotNull
    private URI sourceUri;

    @Column(name = "source_uri_scheme")
    private String sourceUriScheme;

    public String getId() {
        return id;
    }

    public Artwork setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Artwork setDate(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Artwork setMimeType(String mimeType) {
        this.mimeType = requireNonNull(mimeType);
        return this;
    }

    public String getChecksum() {
        return checksum;
    }

    public Artwork setChecksum(String checksum) {
        this.checksum = requireNonNull(checksum);
        return this;
    }

    public long getLargeImageSize() {
        return largeImageSize;
    }

    public Artwork setLargeImageSize(long largeImageSize) {
        this.largeImageSize = largeImageSize;
        return this;
    }

    public String getLargeImagePath() {
        return largeImagePath;
    }

    public Artwork setLargeImagePath(String largeImagePath) {
        this.largeImagePath = requireNonNull(largeImagePath);
        return this;
    }

    public long getSmallImageSize() {
        return smallImageSize;
    }

    public Artwork setSmallImageSize(long smallImageSize) {
        this.smallImageSize = smallImageSize;
        return this;
    }

    public String getSmallImagePath() {
        return smallImagePath;
    }

    public Artwork setSmallImagePath(String smallImagePath) {
        this.smallImagePath = requireNonNull(smallImagePath);
        return this;
    }

    public URI getSourceUri() {
        return sourceUri;
    }

    public Artwork setSourceUri(URI sourceUri) {
        this.sourceUri = requireNonNull(sourceUri);
        sourceUriScheme = sourceUri.getScheme();
        return this;
    }

    @Nullable
    public String getSourceUriScheme() {
        return sourceUriScheme;
    }

    public Artwork setSourceUriScheme(@Nullable String sourceUriScheme) {
        this.sourceUriScheme = sourceUriScheme;
        return this;
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
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("mimeType", mimeType)
                .add("checksum", checksum)
                .add("sourceUri", sourceUri)
                .add("sourceUriScheme", sourceUriScheme)
                .toString();
    }
}
