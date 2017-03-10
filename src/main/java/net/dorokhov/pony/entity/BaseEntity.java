package net.dorokhov.pony.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

@MappedSuperclass
public abstract class BaseEntity<T extends Serializable> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    protected T id;

    @Column(name = "creation_date")
    protected LocalDateTime creationDate;

    @Column(name = "update_date")
    protected LocalDateTime updateDate;

    public Optional<T> getId() {
        return Optional.ofNullable(id);
    }

    public void setId(T id) {
        this.id = id;
    }

    public Optional<LocalDateTime> getCreationDate() {
        return Optional.ofNullable(creationDate);
    }

    protected void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Optional<LocalDateTime> getUpdateDate() {
        return Optional.ofNullable(updateDate);
    }

    protected void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    @PrePersist
    public void prePersist() {
        setCreationDate(LocalDateTime.now());
    }

    @PreUpdate
    public void preUpdate() {
        setUpdateDate(LocalDateTime.now());
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
            BaseEntity that = (BaseEntity) obj;
            return id.equals(that.id);
        }
        return false;
    }
}
