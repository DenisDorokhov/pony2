package net.dorokhov.pony.library.domain;

import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import net.dorokhov.pony.common.SearchableEntity;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

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
    private List<Song> songs = emptyList();

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

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Integer getYear() {
        return year;
    }

    @Nullable
    public Artwork getArtwork() {
        return artwork;
    }

    public List<Song> getSongs() {
        return songs != null ? unmodifiableList(songs) : emptyList();
    }

    public Artist getArtist() {
        return artist;
    }

    @Transient
    @Field(analyzer = @Analyzer(definition = ANALYZER))
    public String getSearchTerms() {
        return Strings.nullToEmpty(name) + " " +
                Strings.nullToEmpty(artist.getName());
    }

    @Override
    public int compareTo(@Nonnull Album album) {
        return ComparisonChain.start()
                .compare(artist, album.artist)
                .compare(year, album.year, Ordering.natural().nullsLast())
                .compare(name, album.name, Ordering.natural().nullsLast())
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

    public static final class Builder {
        
        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String name;
        private Integer year;
        private Artwork artwork;
        private ImmutableList.Builder<Song> songs = ImmutableList.builder();
        private Artist artist;

        private Builder() {
        }

        private Builder(Album album) {
            id = album.id;
            creationDate = album.creationDate;
            updateDate = album.updateDate;
            name = album.name;
            year = album.year;
            artwork = album.artwork;
            songs = ImmutableList.<Song>builder().addAll(album.songs);
            artist = album.artist;
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

        public Builder year(@Nullable Integer year) {
            this.year = year;
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

        public Builder artist(Artist artist) {
            this.artist = artist;
            return this;
        }

        public Album build() {
            return new Album(this);
        }
    }
}
