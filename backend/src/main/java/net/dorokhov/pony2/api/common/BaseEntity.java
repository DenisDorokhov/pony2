package net.dorokhov.pony2.api.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.UUID;

@SuppressWarnings("unchecked")
@MappedSuperclass
abstract public class BaseEntity<T extends BaseEntity<?>> {

    @Id
    @GeneratedValue(strategy = UUID)
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

    public T setId(String id) {
        this.id = id;
        return (T) this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public T setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return (T) this;
    }

    @Nullable
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public T setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
        return (T) this;
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
    @SuppressWarnings("rawtypes")
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
