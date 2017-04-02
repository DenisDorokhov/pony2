package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "config")
public class Config implements Identifiable<String> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @NotNull
    private String id;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime creationDate;

    @Column(name = "update_date", insertable = false)
    private LocalDateTime updateDate;

    @Column(name = "value")
    private String value;

    public Config() {
    }

    public Config(String id) {
        this(id, null);
    }

    public Config(String id, String value) {
        this.id = id;
        this.value = value;
    }

    public Config(String id, int value) {
        this(id, String.valueOf(value));
    }

    public Config(String id, long value) {
        this(id, String.valueOf(value));
    }

    public Config(String id, double value) {
        this(id, String.valueOf(value));
    }

    public Config(String id, boolean value) {
        this(id, String.valueOf(value));
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Optional<LocalDateTime> getCreationDate() {
        return Optional.ofNullable(creationDate);
    }

    void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Optional<LocalDateTime> getUpdateDate() {
        return Optional.ofNullable(updateDate);
    }

    void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValue(Number value) {
        setValue(value != null ? String.valueOf(value) : null);
    }

    public void setValue(Boolean value) {
        setValue(value != null ? String.valueOf(value) : null);
    }

    @Transient
    public Optional<Integer> getInteger() {
        return getValue().map(Integer::valueOf);
    }

    @Transient
    public Optional<Long> getLong() {
        return getValue().map(Long::valueOf);
    }

    @Transient
    public Optional<Double> getDouble() {
        return getValue().map(Double::valueOf);
    }

    @Transient
    public Optional<Boolean> getBoolean() {
        return getValue().map(Boolean::valueOf);
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
    public boolean equals(Object obj) {
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
}
