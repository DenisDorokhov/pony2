package net.dorokhov.pony3.api.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@MappedSuperclass
abstract public class BaseEntity {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    protected String id;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @NotNull
    protected LocalDateTime creationDate;

    @Column(name = "update_date", insertable = false)
    protected LocalDateTime updateDate;

    public String getId() {
        return id;
    }

    public BaseEntity setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public BaseEntity setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    @Nullable
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public BaseEntity setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
        return this;
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
            BaseEntity that = (BaseEntity) obj;
            return id.equals(that.id);
        }
        return false;
    }
}
