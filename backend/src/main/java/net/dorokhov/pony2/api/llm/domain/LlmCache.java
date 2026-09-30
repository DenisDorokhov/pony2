package net.dorokhov.pony2.api.llm.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Table(name = "llm_cache")
public class LlmCache {

    @Id
    @GeneratedValue(strategy = UUID)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private String id;

    @Column(name = "region", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private LlmCacheRegion region;

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

    public LlmCache setId(String id) {
        this.id = id;
        return this;
    }

    public LlmCacheRegion getRegion() {
        return region;
    }

    public LlmCache setRegion(LlmCacheRegion region) {
        this.region = region;
        return this;
    }

    public String getKey() {
        return key;
    }

    public LlmCache setKey(String key) {
        this.key = key;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public LlmCache setVersion(int version) {
        this.version = version;
        return this;
    }

    public String getValue() {
        return value;
    }

    public LlmCache setValue(String value) {
        this.value = value;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LlmCache setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public LlmCache setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }
}
