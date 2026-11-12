package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "artist_discovery")
@DynamicUpdate  // Update only changed columns to support concurrent discovery updates.
public class ArtistDiscovery extends BaseEntity<ArtistDiscovery> {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false, unique = true)
    @NotNull
    private Artist artist;

    @Column(name = "spotify_artist_id")
    private String spotifyArtistId;

    @Column(name = "music_brainz_artist_id")
    private String musicBrainzArtistId;

    @Column(name = "country_code")
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_country_code")
    private CountryCode overrideCountryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bio_external_image_id")
    private ExternalImage bioExternalImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_external_image_id")
    private ExternalImage avatarExternalImage;

    public Artist getArtist() {
        return artist;
    }

    public ArtistDiscovery setArtist(Artist artist) {
        this.artist = artist;
        return this;
    }

    public @Nullable String getSpotifyArtistId() {
        return spotifyArtistId;
    }

    public ArtistDiscovery setSpotifyArtistId(@Nullable String spotifyArtistId) {
        this.spotifyArtistId = spotifyArtistId;
        return this;
    }

    public @Nullable String getMusicBrainzArtistId() {
        return musicBrainzArtistId;
    }

    public ArtistDiscovery setMusicBrainzArtistId(@Nullable String musicBrainzArtistId) {
        this.musicBrainzArtistId = musicBrainzArtistId;
        return this;
    }

    public @Nullable String getCountryCode() {
        return countryCode;
    }

    public ArtistDiscovery setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public @Nullable CountryCode getOverrideCountryCode() {
        return overrideCountryCode;
    }

    public ArtistDiscovery setOverrideCountryCode(@Nullable CountryCode overrideCountryCode) {
        this.overrideCountryCode = overrideCountryCode;
        return this;
    }

    public @Nullable ExternalImage getBioExternalImage() {
        return bioExternalImage;
    }

    public ArtistDiscovery setBioExternalImage(@Nullable ExternalImage bioExternalImage) {
        this.bioExternalImage = bioExternalImage;
        return this;
    }

    public @Nullable ExternalImage getAvatarExternalImage() {
        return avatarExternalImage;
    }

    public ArtistDiscovery setAvatarExternalImage(@Nullable ExternalImage avatarExternalImage) {
        this.avatarExternalImage = avatarExternalImage;
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
                .add("musicBrainzArtistId", musicBrainzArtistId)
                .add("countryCode", countryCode)
                .add("overrideCountryCode", overrideCountryCode)
                .add("bioExternalImage", bioExternalImage)
                .add("avatarExternalImage", avatarExternalImage)
                .toString();
    }
}
