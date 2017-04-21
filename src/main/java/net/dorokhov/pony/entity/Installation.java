package net.dorokhov.pony.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "installation")
public class Installation extends BaseEntity<Long> {

    @Column(name = "version", nullable = false)
    @NotNull
    private String version;
    
    @Column(name = "encryption_key", nullable = false)
    @NotNull
    private String encryptionKey;

    public Installation() {
    }

    private Installation(Builder builder) {
        version = builder.version;
        encryptionKey = builder.encryptionKey;
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
    }

    public String getVersion() {
        return version;
    }

    public String getEncryptionKey() {
        return encryptionKey;
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
        private String encryptionKey;

        public Builder() {
        }
        
        public Builder(Installation installation) {
            version = installation.version;
            encryptionKey = installation.encryptionKey;
            id = installation.id;
            creationDate = installation.creationDate;
            updateDate = installation.updateDate;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder creationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder updateDate(LocalDateTime updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder encryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
            return this;
        }

        public Installation build() {
            return new Installation(this);
        }
    }
}
