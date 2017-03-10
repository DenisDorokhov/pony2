package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Optional;

@Entity
@Table(name = "installation")
public class Installation extends BaseEntity<Long> {

    @Column(name = "version", nullable = false)
    private String version;

    private Installation() {
    }

    public Installation(String version) {
        setVersion(version);
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    public void setVersion(String version) {
        this.version = Preconditions.checkNotNull(version);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("version", version)
                .toString();
    }
}
