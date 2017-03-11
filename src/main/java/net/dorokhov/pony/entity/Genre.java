package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import net.dorokhov.pony.util.OptionalComparator;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import java.util.Optional;

@Entity
@Table(name = "genre")
@Indexed
public class Genre extends BaseEntity<Long> implements Comparable<Genre> {

    @Column(name = "name")
    @Field
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_stored_file_id")
    private StoredFile artwork;

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<StoredFile> getArtwork() {
        return Optional.ofNullable(artwork);
    }

    public void setArtwork(StoredFile artwork) {
        this.artwork = artwork;
    }

    @Override
    public int compareTo(Genre genre) {
        return OptionalComparator.<String>nullLast().compare(getName(), Optional.ofNullable(genre).flatMap(Genre::getName));
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
