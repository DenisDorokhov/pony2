package net.dorokhov.pony.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
abstract public class BaseEntity<T extends Serializable> {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    protected T id;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @NotNull
    protected LocalDateTime creationDate;

    @Column(name = "update_date", insertable = false)
    protected LocalDateTime updateDate;

    public T getId() {
        return id;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    @Nullable
    public LocalDateTime getUpdateDate() {
        return updateDate;
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
