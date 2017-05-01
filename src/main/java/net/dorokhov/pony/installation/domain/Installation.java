package net.dorokhov.pony.installation.domain;

import net.dorokhov.pony.common.BaseEntity;

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
public class Installation extends BaseEntity<Long> implements Serializable {

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
        return "Installation{" +
                "id=" + id +
                ", version='" + version + '\'' +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(Installation installation) {
        return new Builder(installation);
    }

    public static class Builder {
        
        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String version;

        public Builder() {
        }
        
        public Builder(Installation installation) {
            version = installation.version;
            id = installation.id;
            creationDate = installation.creationDate;
            updateDate = installation.updateDate;
        }

        public Builder id(@Nullable Long id) {
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
