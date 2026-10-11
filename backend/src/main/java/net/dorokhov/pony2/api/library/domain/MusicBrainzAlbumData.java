package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.util.List;

public record MusicBrainzAlbumData(
        String releaseGroupId,
        @Nullable String primaryType,
        List<String> secondaryTypes,
        @Nullable String firstReleaseDate,
        String artistId,
        List<String> country
) {}
