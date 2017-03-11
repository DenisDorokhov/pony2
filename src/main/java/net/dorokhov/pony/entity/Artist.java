package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import net.dorokhov.pony.util.OptionalComparator;
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
    @JoinColumn(name = "artwork_stored_file_id")
    private StoredFile artwork;

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

    public Optional<StoredFile> getArtwork() {
        return Optional.ofNullable(artwork);
    }

    public void setArtwork(StoredFile artwork) {
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
        return OptionalComparator.<String>nullLast().compare(
                getName().map(String::toLowerCase).map(s -> s.replaceAll(regex, "")),
                artist.getName().map(String::toLowerCase).map(s -> s.replaceAll(regex, "")));
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
