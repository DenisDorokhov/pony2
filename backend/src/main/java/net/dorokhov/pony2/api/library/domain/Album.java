package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Indexed
@Entity
@Table(name = "album")
public class Album extends BaseEntity<Album> implements Comparable<Album>, Serializable {

    @Column(name = "name")
    private String name;

    @Column(name = "album_year")
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "album")
    private List<Song> songs = new ArrayList<>();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    @NotNull
    private Artist artist;

    @Nullable
    public String getName() {
        return name;
    }

    public Album setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public Integer getYear() {
        return year;
    }

    public Album setYear(@Nullable Integer year) {
        this.year = year;
        return this;
    }

    @Nullable
    public Artwork getArtwork() {
        return artwork;
    }

    public Album setArtwork(@Nullable Artwork artwork) {
        this.artwork = artwork;
        return this;
    }

    public List<Song> getSongs() {
        if (songs == null) {
            songs = new ArrayList<>();
        }
        return songs;
    }

    public Album setSongs(List<Song> songs) {
        this.songs = songs;
        return this;
    }

    public Artist getArtist() {
        return artist;
    }

    public Album setArtist(@NotNull Artist artist) {
        this.artist = requireNonNull(artist);
        return this;
    }

    @Transient
    @FullTextField(name = "searchTerms", analyzer = "ponyAnalyzer")
    @IndexingDependency(derivedFrom = @ObjectPath({
            @PropertyValue(propertyName = "name"),
    }))
    public String getSearchTerms() {
        return Strings.nullToEmpty(name);
    }

    @Transient
    @FullTextField(name = "fallbackSearchTerms", analyzer = "ponyAnalyzer")
    @IndexingDependency(derivedFrom = @ObjectPath({
            @PropertyValue(propertyName = "name"),
    }))
    @IndexingDependency(derivedFrom = @ObjectPath({
            @PropertyValue(propertyName = "year"),
    }))
    @IndexingDependency(derivedFrom = @ObjectPath({
            @PropertyValue(propertyName = "artist"),
            @PropertyValue(propertyName = "name"),
    }))
    public String getFallbackSearchTerms() {
        return Strings.nullToEmpty(name) + " " +
                Strings.nullToEmpty(year != null ? String.valueOf(year) : "") + " " +
                Strings.nullToEmpty(artist.getName());
    }

    @Override
    public int compareTo(Album album) {
        return ComparisonChain.start()
                .compare(getArtist(), album.getArtist())
                .compare(year, album.year, Ordering.natural().nullsLast())
                .compare(name, album.name, Ordering.natural().nullsLast())
                .result();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("year", year)
                .add("artwork", artwork)
                .add("artist", artist)
                .toString();
    }
}
