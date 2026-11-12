package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.util.List;

public record ArtistDiscoveryData(
        @Nullable String countryCode,
        List<String> topSongIds,
        @Nullable String avatarExternalImageId,
        @Nullable String spotifyArtistId,
        @Nullable CountryCode overrideCountryCode,
        @Nullable String musicBrainzArtistId,
        @Nullable String bioExternalImageId
) {}
