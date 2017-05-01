package net.dorokhov.pony.library.domain;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import net.dorokhov.pony.common.SearchableEntity;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "genre")
@Indexed
public class Genre extends SearchableEntity<Long> implements Comparable<Genre>, Serializable {

    @Column(name = "name")
    @Field(analyzer = @Analyzer(definition = ANALYZER))
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    protected Genre() {
    }

    private Genre(Builder builder) {
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
        name = builder.name;
        artwork = builder.artwork;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Artwork getArtwork() {
        return artwork;
    }

    @Override
    public int compareTo(@Nonnull Genre genre) {
        return ComparisonChain.start()
                .compare(name, genre.getName(), Ordering.natural().nullsLast())
                .result();
    }

    @Override
    public String toString() {
        return "Genre{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", artwork=" + artwork +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(Genre genre) {
        return new Builder(genre);
    }

    public static class Builder {
        
        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String name;
        private Artwork artwork;

        public Builder() {
        }
        
        public Builder(Genre genre) {
            name = genre.name;
            artwork = genre.artwork;
            id = genre.id;
            creationDate = genre.creationDate;
            updateDate = genre.updateDate;
        }

        public Builder id(@Nullable Long id) {
            this.id = id;
            return this;
        }

        public Builder creationDate(@Nullable LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder updateDate(@Nullable LocalDateTime updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        public Builder artwork(@Nullable Artwork artwork) {
            this.artwork = artwork;
            return this;
        }

        public Genre build() {
            return new Genre(this);
        }
    }
}
