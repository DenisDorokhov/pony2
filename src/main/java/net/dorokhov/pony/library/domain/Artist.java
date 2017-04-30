package net.dorokhov.pony.library.domain;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.common.persistence.SearchableEntity;
import net.dorokhov.pony.common.OptionalComparators;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private List<Album> albums = ImmutableList.of();

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

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<Artwork> getArtwork() {
        return Optional.ofNullable(artwork);
    }

    public List<Album> getAlbums() {
        return albums != null ? albums : ImmutableList.of();
    }

    @Override
    public int compareTo(Artist artist) {
        String regex = "^the\\s+";
        return OptionalComparators.<String>nullLast().compare(
                getName().map(String::toLowerCase).map(s -> s.replaceAll(regex, "")),
                artist.getName().map(String::toLowerCase).map(s -> s.replaceAll(regex, "")));
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

    public static class Builder {
        
        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String name;
        private ImmutableList.Builder<Album> albums = ImmutableList.builder();
        private Artwork artwork;

        public Builder() {
        }
        
        public Builder(Artist artist) {
            id = artist.id;
            creationDate = artist.creationDate;
            updateDate = artist.updateDate;
            name = artist.name;
            albums = ImmutableList.<Album>builder().addAll(artist.albums);
            artwork = artist.artwork;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder creationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder updateDate(LocalDateTime updateDate) {
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

        public Builder albums(List<Album> albums) {
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
