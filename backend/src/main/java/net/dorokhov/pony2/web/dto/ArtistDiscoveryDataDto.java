package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;
import net.dorokhov.pony2.api.library.domain.CountryCode;

import java.util.List;

public final class ArtistDiscoveryDataDto {

    private String countryCode;
    private CountryCode overrideCountryCode;
    private List<String> topSongIds;
    private String avatarExternalImageId;
    private String spotifyArtistId;

    public @Nullable String getCountryCode() {
        return countryCode;
    }

    public ArtistDiscoveryDataDto setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public @Nullable CountryCode getOverrideCountryCode() {
        return overrideCountryCode;
    }

    public ArtistDiscoveryDataDto setOverrideCountryCode(@Nullable CountryCode overrideCountryCode) {
        this.overrideCountryCode = overrideCountryCode;
        return this;
    }

    public List<String> getTopSongIds() {
        return topSongIds;
    }

    public ArtistDiscoveryDataDto setTopSongIds(List<String> topSongIds) {
        this.topSongIds = topSongIds;
        return this;
    }

    public @Nullable String getAvatarExternalImageId() {
        return avatarExternalImageId;
    }

    public ArtistDiscoveryDataDto setAvatarExternalImageId(@Nullable String avatarExternalImageId) {
        this.avatarExternalImageId = avatarExternalImageId;
        return this;
    }

    public @Nullable String getSpotifyArtistId() {
        return spotifyArtistId;
    }

    public ArtistDiscoveryDataDto setSpotifyArtistId(@Nullable String spotifyArtistId) {
        this.spotifyArtistId = spotifyArtistId;
        return this;
    }

    public static ArtistDiscoveryDataDto of(ArtistDiscoveryData data) {
        return new ArtistDiscoveryDataDto()
                .setCountryCode(data.countryCode())
                .setOverrideCountryCode(data.overrideCountryCode())
                .setTopSongIds(data.topSongIds())
                .setAvatarExternalImageId(data.avatarExternalImageId())
                .setSpotifyArtistId(data.spotifyArtistId());
    }
}
