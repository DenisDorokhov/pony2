package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import net.dorokhov.pony2.api.common.BaseEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Indexed
@Entity
@Table(name = "artist")
public class Artist extends BaseEntity<Artist> implements Comparable<Artist>, Serializable {

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "artist")
    private List<Album> albums = new ArrayList<>();

    @Nullable
    public String getName() {
        return name;
    }

    public Artist setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public Artwork getArtwork() {
        return artwork;
    }

    public Artist setArtwork(@Nullable Artwork artwork) {
        this.artwork = artwork;
        return this;
    }

    public List<Album> getAlbums() {
        if (albums == null) {
            albums = new ArrayList<>();
        }
        return albums;
    }

    public Artist setAlbums(List<Album> albums) {
        this.albums = albums;
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

    @Override
    public int compareTo(Artist artist) {
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
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("name", name)
                .add("artwork", artwork)
                .toString();
    }
}
