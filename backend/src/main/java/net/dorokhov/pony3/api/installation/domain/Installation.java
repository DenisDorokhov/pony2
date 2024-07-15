package net.dorokhov.pony3.api.installation.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony3.api.common.BaseEntity;

import java.io.Serializable;

@Entity
@Table(name = "installation")
public class Installation extends BaseEntity<Installation> implements Serializable {

    @Column(name = "version", nullable = false)
    @NotNull
    private String version;

    public String getVersion() {
        return version;
    }

    public Installation setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("version", version)
                .toString();
    }
}
