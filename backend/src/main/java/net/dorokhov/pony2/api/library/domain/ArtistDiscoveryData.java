package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.util.List;

public record ArtistDiscoveryData(@Nullable String countryCode, List<String> topSongIds) {}
