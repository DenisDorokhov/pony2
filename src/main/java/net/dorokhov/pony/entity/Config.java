package net.dorokhov.pony.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "config")
public class Config implements Identity<String>, Serializable {

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

    private Config(Builder builder) {
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
        value = builder.value;
    }

    @Override
    public String getId() {
        return id;
    }

    public Optional<LocalDateTime> getCreationDate() {
        return Optional.ofNullable(creationDate);
    }

    public Optional<LocalDateTime> getUpdateDate() {
        return Optional.ofNullable(updateDate);
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(value);
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
        return "Config{" +
                "id='" + id + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(Config config) {
        return new Builder(config);
    }

    public static class Builder {
        
        private String id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String value;

        public Builder() {
        }
        
        public Builder(Config config) {
            id = config.id;
            creationDate = config.creationDate;
            updateDate = config.updateDate;
            value = config.value;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        Builder creationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        Builder updateDate(LocalDateTime updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder value(Number value) {
            value(value != null ? String.valueOf(value) : null);
            return this;
        }

        public Builder value(Boolean value) {
            value(value != null ? String.valueOf(value) : null);
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}
