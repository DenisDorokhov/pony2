package net.dorokhov.pony.entity;

import net.dorokhov.pony.util.OptionalComparators;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "genre")
@Indexed
public class Genre extends BaseEntity<Long> implements Comparable<Genre>, Serializable {

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

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<Artwork> getArtwork() {
        return Optional.ofNullable(artwork);
    }

    @Override
    public int compareTo(Genre genre) {
        return OptionalComparators.<String>nullLast()
                .compare(getName(), Optional.ofNullable(genre).flatMap(Genre::getName));
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

        Builder id(Long id) {
            this.id = id;
            return this;
        }

        Builder creationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        Builder updateDate(LocalDateTime updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder artwork(Artwork artwork) {
            this.artwork = artwork;
            return this;
        }

        public Genre build() {
            return new Genre(this);
        }
    }
}
