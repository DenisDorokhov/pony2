package net.dorokhov.pony3.api.config.domain;

import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "config")
public class Config implements Serializable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @NotNull
    private String id;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime creationDate;

    @Column(name = "update_date", insertable = false)
    private LocalDateTime updateDate;

    @Column(name = "config_value")
    private String value;

    public String getId() {
        return id;
    }

    public Config setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Config setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    @Nullable
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public Config setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public Config setValue(@Nullable String value) {
        this.value = value;
        return this;
    }

    public Config setValue(@Nullable Number value) {
        setValue(value != null ? String.valueOf(value) : null);
        return this;
    }

    public Config setValue(@Nullable Boolean value) {
        setValue(value != null ? String.valueOf(value) : null);
        return this;
    }

    @Transient
    @Nullable
    public Integer getInteger() {
        return value != null ? Integer.valueOf(value) : null;
    }

    @Transient
    @Nullable
    public Long getLong() {
        return value != null ? Long.valueOf(value) : null;
    }

    @Transient
    @Nullable
    public Double getDouble() {
        return value != null ? Double.valueOf(value) : null;
    }

    @Transient
    @Nullable
    public Boolean getBoolean() {
        return value != null ? Boolean.valueOf(value) : null;
    }

    @PrePersist
    public void prePersist() {
        creationDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updateDate = LocalDateTime.now();
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
            Config that = (Config) obj;
            return id.equals(that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("value", value)
                .toString();
    }

    public static Config of(String id, String value) {
        return new Config()
                .setId(id)
                .setValue(value);
    }

    public static Config of(String id, int value) {
        return new Config()
                .setId(id)
                .setValue(value);
    }

    public static Config of(String id, long value) {
        return new Config()
                .setId(id)
                .setValue(value);
    }

    public static Config of(String id, double value) {
        return new Config()
                .setId(id)
                .setValue(value);
    }

    public static Config of(String id, boolean value) {
        return new Config()
                .setId(id)
                .setValue(value);
    }
}
