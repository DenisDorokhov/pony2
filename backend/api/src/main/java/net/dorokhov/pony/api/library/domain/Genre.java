package net.dorokhov.pony.api.library.domain;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import net.dorokhov.pony.api.common.SearchableEntity;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "genre")
    private List<Song> songs = emptyList();

    protected Genre() {
    }

    private Genre(Builder builder) {
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
        name = builder.name;
        artwork = builder.artwork;
        songs = builder.songs.build();
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Artwork getArtwork() {
        return artwork;
    }

    public List<Song> getSongs() {
        return songs != null ? unmodifiableList(songs) : emptyList();
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

    public static final class Builder {
        
        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String name;
        private Artwork artwork;
        private ImmutableList.Builder<Song> songs = ImmutableList.builder();

        private Builder() {
        }
        
        private Builder(Genre genre) {
            id = genre.getId();
            creationDate = genre.getCreationDate();
            updateDate = genre.getUpdateDate();
            name = genre.getName();
            artwork = genre.getArtwork();
            songs = ImmutableList.<Song>builder().addAll(genre.getSongs());
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

        public Builder songs(@Nullable List<Song> songs) {
            if (songs != null) {
                this.songs = ImmutableList.<Song>builder().addAll(songs);
            } else {
                this.songs = ImmutableList.builder();
            }
            return this;
        }

        public Genre build() {
            return new Genre(this);
        }
    }
}
