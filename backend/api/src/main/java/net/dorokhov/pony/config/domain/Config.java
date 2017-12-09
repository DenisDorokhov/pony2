package net.dorokhov.pony.config.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.google.common.base.Preconditions.checkNotNull;

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

    @Column(name = "value")
    private String value;

    protected Config() {
    }

    private Config(Builder builder) {
        this.id = checkNotNull(builder.id);
        this.value = builder.value;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    @Nullable
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    @Nullable
    public String getValue() {
        return value;
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
        return "Config{" +
                "id='" + id + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public static Config of(String id, String value) {
        return builder().id(id).value(value).build();
    }

    public static Config of(String id, int value) {
        return builder().id(id).value(value).build();
    }

    public static Config of(String id, long value) {
        return builder().id(id).value(value).build();
    }

    public static Config of(String id, double value) {
        return builder().id(id).value(value).build();
    }

    public static Config of(String id, boolean value) {
        return builder().id(id).value(value).build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(Config config) {
        return new Builder(config);
    }

    public static final class Builder {
        
        private String id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String value;

        private Builder() {
        }
        
        private Builder(Config config) {
            id = config.getId();
            creationDate = config.getCreationDate();
            updateDate = config.getUpdateDate();
            value = config.getValue();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder creationDate(@Nullable LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder updateDate(@Nullable LocalDateTime updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public Builder value(@Nullable String value) {
            this.value = value;
            return this;
        }

        public Builder value(@Nullable Number value) {
            value(value != null ? String.valueOf(value) : null);
            return this;
        }

        public Builder value(@Nullable Boolean value) {
            value(value != null ? String.valueOf(value) : null);
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}
