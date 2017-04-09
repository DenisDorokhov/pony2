package net.dorokhov.pony.entity;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

import static net.dorokhov.pony.entity.BaseEntity.ANALYZER;

@MappedSuperclass
@AnalyzerDef(name = ANALYZER,
        tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
        filters = {
                @TokenFilterDef(factory = LowerCaseFilterFactory.class),
                @TokenFilterDef(factory = StandardFilterFactory.class)
        })
public abstract class BaseEntity<T extends Serializable> implements Identifiable<T> {
    
    static final String ANALYZER = "noStopWordsAnalyzer";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    protected T id;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @NotNull
    protected LocalDateTime creationDate;

    @Column(name = "update_date", insertable = false)
    protected LocalDateTime updateDate;

    @Override
    public T getId() {
        return id;
    }

    void setId(T id) {
        this.id = id;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Optional<LocalDateTime> getUpdateDate() {
        return Optional.ofNullable(updateDate);
    }

    void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
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
