package net.dorokhov.pony.api.library.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import net.dorokhov.pony.api.common.SearchableEntity;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "artist")
@Indexed
public class Artist extends SearchableEntity<Long> implements Comparable<Artist>, Serializable {

    @Column(name = "name")
    @Field(analyzer = @Analyzer(definition = ANALYZER))
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "artist")
    private List<Album> albums = emptyList();

    protected Artist() {
    }

    private Artist(Builder builder) {
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
        name = builder.name;
        artwork = builder.artwork;
        albums = builder.albums.build();
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Artwork getArtwork() {
        return artwork;
    }

    public List<Album> getAlbums() {
        return albums != null ? unmodifiableList(albums) : emptyList();
    }

    @Override
    public int compareTo(@Nonnull Artist artist) {
        String regex = "^the\\s+";
        return Objects.compare(
                Optional.ofNullable(name)
                        .map(String::toLowerCase)
                        .map(s -> s.replaceAll(regex, ""))
                        .orElse(null),
                Optional.ofNullable(artist.name)
                        .map(String::toLowerCase)
                        .map(s -> s.replaceAll(regex, ""))
                        .orElse(null), 
                Ordering.natural().nullsLast()
        );
    }

    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", artwork=" + artwork +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(Artist artist) {
        return new Builder(artist);
    }

    public static final class Builder {
        
        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String name;
        private ImmutableList.Builder<Album> albums = ImmutableList.builder();
        private Artwork artwork;

        private Builder() {
        }
        
        private Builder(Artist artist) {
            id = artist.getId();
            creationDate = artist.getCreationDate();
            updateDate = artist.getUpdateDate();
            name = artist.getName();
            artwork = artist.getArtwork();
            albums = ImmutableList.<Album>builder().addAll(artist.getAlbums());
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

        public Builder albums(@Nullable List<Album> albums) {
            if (albums != null) {
                this.albums = ImmutableList.<Album>builder().addAll(albums);
            } else {
                this.albums = ImmutableList.builder();
            }
            return this;
        }

        public Artist build() {
            return new Artist(this);
        }
    }
}
