package net.dorokhov.pony2.api.llm.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Table(name = "discovery_cache")
public class DiscoveryCache {

    @Id
    @GeneratedValue(strategy = UUID)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private String id;

    @Column(name = "region", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private DiscoveryCacheRegion region;

    @Column(name = "`key`", nullable = false)
    private String key;

    @Column(name = "`version`", nullable = false)
    private int version;

    @Column(name = "`value`")
    private String value;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    public String getId() {
        return id;
    }

    public DiscoveryCache setId(String id) {
        this.id = id;
        return this;
    }

    public DiscoveryCacheRegion getRegion() {
        return region;
    }

    public DiscoveryCache setRegion(DiscoveryCacheRegion region) {
        this.region = region;
        return this;
    }

    public String getKey() {
        return key;
    }

    public DiscoveryCache setKey(String key) {
        this.key = key;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public DiscoveryCache setVersion(int version) {
        this.version = version;
        return this;
    }

    public String getValue() {
        return value;
    }

    public DiscoveryCache setValue(String value) {
        this.value = value;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public DiscoveryCache setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public DiscoveryCache setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }
}
