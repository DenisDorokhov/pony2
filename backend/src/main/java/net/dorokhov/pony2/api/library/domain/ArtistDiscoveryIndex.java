package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;

@Entity
@Table(name = "artist_discovery_index")
public class ArtistDiscoveryIndex extends BaseEntity<ArtistDiscoveryIndex> {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false, unique = true)
    @NotNull
    private Artist artist;

    @Column(name = "spotify_artist_id")
    private String spotifyArtistId;

    public Artist getArtist() {
        return artist;
    }

    public ArtistDiscoveryIndex setArtist(Artist artist) {
        this.artist = artist;
        return this;
    }

    @Nullable
    public String getSpotifyArtistId() {
        return spotifyArtistId;
    }

    public ArtistDiscoveryIndex setSpotifyArtistId(@Nullable String spotifyArtistId) {
        this.spotifyArtistId = spotifyArtistId;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("artist", artist != null ? artist.getId() : "null")
                .add("spotifyArtistId", spotifyArtistId)
                .toString();
    }
}
