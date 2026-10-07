package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SpotifyArtistData(
        @NotNull
        Status status,
        @Nullable
        @Valid
        SpotifyArtist artist,
        @Nullable
        @Valid
        MatchedAlbum matchedAlbum,
        @Nullable
        List<@NotNull @Valid TopTrack> topTracks,
        @Nullable
        List<@NotNull @Valid SpotifyArtist> similarArtists,
        @Nullable
        String biography,
        @Nullable
        List<@NotNull @Valid ExternalLink> links,
        @Nullable
        String imageUrl
) {

    public enum Status {
        FOUND, NOT_FOUND, AMBIGUOUS
    }

    public record SpotifyArtist(
            @NotBlank
            String id,
            @NotBlank
            String name,
            @NotBlank
            String url
    ) {}

    public record MatchedAlbum(
            @NotBlank
            String inputTitle,
            @NotBlank
            String title,
            @NotBlank
            String id,
            @NotBlank
            String url
    ) {}

    public record TopTrack(
            @NotBlank
            String id,
            @NotBlank
            String title,
            @NotBlank
            String url,
            @NotBlank
            String albumTitle,
            @NotBlank
            String albumId,
            @NotBlank
            String albumUrl
    ) {}

    public record ExternalLink(
            @NotBlank
            String title,
            @NotBlank
            String url
    ) {}
}
