package net.dorokhov.pony3.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import net.dorokhov.pony3.api.common.BaseEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

@Entity
@Table(name = "genre")
public class Genre extends BaseEntity implements Comparable<Genre>, Serializable {

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "genre")
    private List<Song> songs = emptyList();

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
