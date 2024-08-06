package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import net.dorokhov.pony2.api.common.BaseEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Indexed
@Entity
@Table(name = "genre")
public class Genre extends BaseEntity<Genre> implements Comparable<Genre>, Serializable {

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "genre")
    private List<Song> songs = new ArrayList<>();

    @Nullable
    public String getName() {
        return name;
    }

    public Genre setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public Artwork getArtwork() {
        return artwork;
    }

    public Genre setArtwork(@Nullable Artwork artwork) {
        this.artwork = artwork;
        return this;
    }

    public List<Song> getSongs() {
        if (songs == null) {
            songs = new ArrayList<>();
        }
        return songs;
    }

    public Genre setSongs(List<Song> songs) {
        this.songs = songs;
        return this;
    }

    @Transient
    @FullTextField(name = "searchTerms")
    @IndexingDependency(derivedFrom = @ObjectPath({
            @PropertyValue(propertyName = "name"),
    }))
    public String getSearchTerms() {
        return Strings.nullToEmpty(name);
    }

    @Override
    public int compareTo(Genre genre) {
        return ComparisonChain.start()
                .compare(name, genre.getName(), Ordering.natural().nullsLast())
                .result();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("artwork", artwork)
                .toString();
    }
}
