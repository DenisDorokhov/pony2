package net.dorokhov.pony.api.installation.domain;

import com.google.common.base.MoreObjects;
import net.dorokhov.pony.api.common.BaseEntity;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "installation")
public class Installation extends BaseEntity implements Serializable {

    @Column(name = "version", nullable = false)
    @NotNull
    private String version;

    protected Installation() {
    }

    private Installation(Builder builder) {
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
        version = checkNotNull(builder.version);
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("version", version)
                .toString();
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(Installation installation) {
        return new Builder(installation);
    }

    public static final class Builder {
        
        private String id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String version;

        private Builder() {
        }
        
        private Builder(Installation installation) {
            id = installation.getId();
            creationDate = installation.getCreationDate();
            updateDate = installation.getUpdateDate();
            version = installation.getVersion();
        }

        public Builder id(@Nullable String id) {
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

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Installation build() {
            return new Installation(this);
        }
    }
}
