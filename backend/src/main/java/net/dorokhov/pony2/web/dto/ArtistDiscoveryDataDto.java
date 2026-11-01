package net.dorokhov.pony2.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;

import java.util.List;

public final class ArtistDiscoveryDataDto {

    private String countryCode;
    private List<String> topSongIds;
    private String bioExternalImageId;
    private String avatarExternalImageId;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public @Nullable String getCountryCode() {
        return countryCode;
    }

    public ArtistDiscoveryDataDto setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public List<String> getTopSongIds() {
        return topSongIds;
    }

    public ArtistDiscoveryDataDto setTopSongIds(List<String> topSongIds) {
        this.topSongIds = topSongIds;
        return this;
    }

    public @Nullable String getBioExternalImageId() {
        return bioExternalImageId;
    }

    public ArtistDiscoveryDataDto setBioExternalImageId(@Nullable String bioExternalImageId) {
        this.bioExternalImageId = bioExternalImageId;
        return this;
    }

    public @Nullable String getAvatarExternalImageId() {
        return avatarExternalImageId;
    }

    public ArtistDiscoveryDataDto setAvatarExternalImageId(@Nullable String avatarExternalImageId) {
        this.avatarExternalImageId = avatarExternalImageId;
        return this;
    }

    public static ArtistDiscoveryDataDto of(ArtistDiscoveryData data) {
        return new ArtistDiscoveryDataDto()
                .setCountryCode(data.countryCode())
                .setTopSongIds(data.topSongIds())
                .setBioExternalImageId(data.bioExternalImageId())
                .setAvatarExternalImageId(data.avatarExternalImageId());
    }
}
