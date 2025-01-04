package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Table(name = "artist_genre")
public class ArtistGenre implements Comparable<ArtistGenre> {

    @Id
    @GeneratedValue(strategy = UUID)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private String id;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime creationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    @NotNull
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    @NotNull
    private Genre genre;

    public String getId() {
        return id;
    }

    public ArtistGenre setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public ArtistGenre setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public Artist getArtist() {
        return artist;
    }

    public ArtistGenre setArtist(Artist artist) {
        this.artist = artist;
        return this;
    }

    public Genre getGenre() {
        return genre;
    }

    public ArtistGenre setGenre(Genre genre) {
        this.genre = genre;
        return this;
    }

    @PrePersist
    public void prePersist() {
        creationDate = LocalDateTime.now();
    }

    @Override
    public int compareTo(ArtistGenre artistGenre) {
        return getGenre().compareTo(artistGenre.getGenre());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
