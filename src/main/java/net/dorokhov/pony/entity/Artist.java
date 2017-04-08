package net.dorokhov.pony.entity;

import net.dorokhov.pony.util.OptionalComparators;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "artist")
@Indexed
public class Artist extends BaseEntity<Long> implements Comparable<Artist> {

    @Column(name = "name")
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "artist")
    private List<Album> albums;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Album> getAlbums() {
        if (albums == null) {
            albums = new ArrayList<>();
        }
        return albums;
    }

    public void setAlbums(List<Album> albums) {
        this.albums = albums;
    }

    public Optional<Artwork> getArtwork() {
        return Optional.ofNullable(artwork);
    }

    public void setArtwork(Artwork artwork) {
        this.artwork = artwork;
    }

    @Transient
    @Field
    public String getSearchTerms() {
        return getName().orElse("");
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
}
