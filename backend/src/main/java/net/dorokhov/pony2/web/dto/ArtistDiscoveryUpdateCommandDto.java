package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import net.dorokhov.pony2.api.library.domain.CountryCode;

public final class ArtistDiscoveryUpdateCommandDto {

    @NotBlank
    private String artistId;

    private CountryCode overrideCountryCode;

    public String getArtistId() {
        return artistId;
    }

    public ArtistDiscoveryUpdateCommandDto setArtistId(String artistId) {
        this.artistId = artistId;
        return this;
    }

    public @Nullable CountryCode getOverrideCountryCode() {
        return overrideCountryCode;
    }

    public ArtistDiscoveryUpdateCommandDto setOverrideCountryCode(@Nullable CountryCode overrideCountryCode) {
        this.overrideCountryCode = overrideCountryCode;
        return this;
    }
}
