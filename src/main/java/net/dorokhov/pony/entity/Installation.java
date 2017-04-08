package net.dorokhov.pony.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "installation")
public class Installation extends BaseEntity<Long> {

    @Column(name = "version", nullable = false)
    @NotNull
    private String version;

    public Installation() {
    }

    public Installation(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Installation{" +
                "id=" + id +
                ", version='" + version + '\'' +
                '}';
    }
}
