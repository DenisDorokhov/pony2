package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import net.dorokhov.pony.util.OptionalComparators;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "album")
@Indexed
public class Album extends BaseEntity<Long> implements Comparable<Album> {

    @Column(name = "name")
    private String name;

    @Column(name = "year")
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "album")
    private List<Song> songs;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    @NotNull
    private Artist artist;

    public Album() {
    }

    public Album(Artist artist) {
        this.artist = artist;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<Integer> getYear() {
        return Optional.ofNullable(year);
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Optional<Artwork> getArtwork() {
        return Optional.ofNullable(artwork);
    }

    public void setArtwork(Artwork artwork) {
        this.artwork = artwork;
    }

    public List<Song> getSongs() {
        if (songs == null) {
            songs = new ArrayList<>();
        }
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    @Transient
    @Field
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
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("year", year)
                .add("artwork", artwork)
                .add("artist", artist)
                .toString();
    }
}
