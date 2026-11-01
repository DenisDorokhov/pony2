package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.net.URI;
import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Table(name = "external_image")
public class ExternalImage {

    @Id
    @GeneratedValue(strategy = UUID)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private String id;

    @Column(name = "date", nullable = false)
    @NotNull
    private LocalDateTime date;

    @Column(name = "source_uri", nullable = false, length = 2048)
    @NotNull
    private String sourceUri;

    @Column(name = "path", nullable = false, unique = true)
    @NotNull
    private String path;

    @Column(name = "mime_type", nullable = false)
    @NotNull
    private String mimeType;

    @Column(name = "size", nullable = false)
    private long size;

    public String getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public URI getSourceUri() {
        return URI.create(sourceUri);
    }

    public ExternalImage setSourceUri(URI sourceUri) {
        this.sourceUri = sourceUri.toString();
        return this;
    }

    public String getPath() {
        return path;
    }

    public ExternalImage setPath(String path) {
        this.path = path;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public ExternalImage setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public long getSize() {
        return size;
    }

    public ExternalImage setSize(long size) {
        this.size = size;
        return this;
    }

    @PrePersist
    public void prePersist() {
        date = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("date", date)
                .add("sourceUri", sourceUri)
                .add("path", path)
                .add("mimeType", mimeType)
                .add("size", size)
                .toString();
    }
}
