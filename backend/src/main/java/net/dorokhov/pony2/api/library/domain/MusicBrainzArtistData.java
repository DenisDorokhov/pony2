package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

public record MusicBrainzArtistData(String artistId, @Nullable String countryCode) {}
