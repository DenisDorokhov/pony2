package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;
import net.dorokhov.pony2.api.library.domain.CountryCode;

public final class ArtistDiscoveryDto {

    private String spotifyArtistId;
    private String musicBrainzArtistId;
    private String countryCode;
    private String bioExternalImageId;
    private String avatarExternalImageId;
    private CountryCode overrideCountryCode;

    public @Nullable String getSpotifyArtistId() {
        return spotifyArtistId;
    }

    public ArtistDiscoveryDto setSpotifyArtistId(@Nullable String spotifyArtistId) {
        this.spotifyArtistId = spotifyArtistId;
        return this;
    }

    public @Nullable String getMusicBrainzArtistId() {
        return musicBrainzArtistId;
    }

    public ArtistDiscoveryDto setMusicBrainzArtistId(@Nullable String musicBrainzArtistId) {
        this.musicBrainzArtistId = musicBrainzArtistId;
        return this;
    }

    public @Nullable String getCountryCode() {
        return countryCode;
    }

    public ArtistDiscoveryDto setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public @Nullable String getBioExternalImageId() {
        return bioExternalImageId;
    }

    public ArtistDiscoveryDto setBioExternalImageId(@Nullable String bioExternalImageId) {
        this.bioExternalImageId = bioExternalImageId;
        return this;
    }

    public @Nullable String getAvatarExternalImageId() {
        return avatarExternalImageId;
    }

    public ArtistDiscoveryDto setAvatarExternalImageId(@Nullable String avatarExternalImageId) {
        this.avatarExternalImageId = avatarExternalImageId;
        return this;
    }

    public @Nullable CountryCode getOverrideCountryCode() {
        return overrideCountryCode;
    }

    public ArtistDiscoveryDto setOverrideCountryCode(@Nullable CountryCode overrideCountryCode) {
        this.overrideCountryCode = overrideCountryCode;
        return this;
    }

    public static ArtistDiscoveryDto of(@Nullable ArtistDiscoveryData data) {
        if (data == null) {
            return new ArtistDiscoveryDto();
        }
        return new ArtistDiscoveryDto()
                .setSpotifyArtistId(data.spotifyArtistId())
                .setMusicBrainzArtistId(data.musicBrainzArtistId())
                .setCountryCode(data.countryCode())
                .setBioExternalImageId(data.bioExternalImageId())
                .setAvatarExternalImageId(data.avatarExternalImageId())
                .setOverrideCountryCode(data.overrideCountryCode());
    }
}
