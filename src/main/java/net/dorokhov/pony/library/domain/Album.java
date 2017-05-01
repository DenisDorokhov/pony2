package net.dorokhov.pony.library.domain;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.common.SearchableEntity;
import net.dorokhov.pony.common.OptionalComparators;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "album")
@Indexed
public class Album extends SearchableEntity<Long> implements Comparable<Album>, Serializable {

    @Column(name = "name")
    private String name;

    @Column(name = "year")
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "album")
    private List<Song> songs = ImmutableList.of();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    @NotNull
    private Artist artist;

    protected Album() {
    }

    private Album(Builder builder) {
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
        name = builder.name;
        year = builder.year;
        artwork = builder.artwork;
        songs = builder.songs.build();
        artist = checkNotNull(builder.artist);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<Integer> getYear() {
        return Optional.ofNullable(year);
    }

    public Optional<Artwork> getArtwork() {
        return Optional.ofNullable(artwork);
    }

    public List<Song> getSongs() {
        return songs != null ? songs : ImmutableList.of();
    }

    public Artist getArtist() {
        return artist;
    }

    @Transient
    @Field(analyzer = @Analyzer(definition = ANALYZER))
    public String getSearchTerms() {
        return getName().orElse("") + " " +
                getArtist().getName().orElse("");
    }

    @Override
    public int compareTo(Album album) {
        return ComparisonChain.start()
                .compare(getArtist(), album.getArtist())
                .compare(getYear(), album.getYear(), OptionalComparators.nullLast())
                .compare(getName(), album.getName(), OptionalComparators.nullLast())
                .result();
    }

    @Override
    public String toString() {
        return "Album{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", year=" + year +
                ", artwork=" + artwork +
                ", artist=" + artist +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(Album album) {
        return new Builder(album);
    }

    public static class Builder {
        
        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String name;
        private Integer year;
        private Artwork artwork;
        private ImmutableList.Builder<Song> songs = ImmutableList.builder();
        private Artist artist;

        public Builder() {
        }

        private Builder(Album builder) {
            id = builder.id;
            creationDate = builder.creationDate;
            updateDate = builder.updateDate;
            name = builder.name;
            year = builder.year;
            artwork = builder.artwork;
            songs = ImmutableList.<Song>builder().addAll(builder.songs);
            artist = builder.artist;
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

        public Builder year(Integer year) {
            this.year = year;
            return this;
        }

        public Builder artwork(Artwork artwork) {
            this.artwork = artwork;
            return this;
        }

        public Builder songs(List<Song> songs) {
            if (songs != null) {
                this.songs = ImmutableList.<Song>builder().addAll(songs);
            } else {
                this.songs = ImmutableList.builder();
            }
            return this;
        }

        public Builder artist(Artist artist) {
            this.artist = artist;
            return this;
        }

        public Album build() {
            return new Album(this);
        }
    }
}
