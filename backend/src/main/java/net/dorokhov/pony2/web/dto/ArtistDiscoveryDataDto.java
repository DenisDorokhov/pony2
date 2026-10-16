package net.dorokhov.pony2.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;

import java.util.List;

public final class ArtistDiscoveryDataDto {

    private String countryCode;
    private List<String> topSongIds;

    @Nullable
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public String getCountryCode() {
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

    public static ArtistDiscoveryDataDto of(ArtistDiscoveryData data) {
        return new ArtistDiscoveryDataDto()
                .setCountryCode(data.countryCode())
                .setTopSongIds(data.topSongIds());
    }
}
