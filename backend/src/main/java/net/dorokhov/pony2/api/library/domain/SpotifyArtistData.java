package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("status", status)
                .add("artist", artist)
                .add("matchedAlbum", matchedAlbum)
                .add("topTracks", topTracks)
                .add("similarArtists", similarArtists)
                .add("biography", biography)
                .add("links", links)
                .add("imageUrl", imageUrl)
                .toString();
    }

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
    ) {

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("name", name)
                    .add("url", url)
                    .toString();
        }
    }

    public record MatchedAlbum(
            @NotBlank
            String inputTitle,
            @NotBlank
            String title,
            @NotBlank
            String id,
            @NotBlank
            String url
    ) {

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("inputTitle", inputTitle)
                    .add("title", title)
                    .add("id", id)
                    .add("url", url)
                    .toString();
        }
    }

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
    ) {

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("title", title)
                    .add("url", url)
                    .add("albumTitle", albumTitle)
                    .add("albumId", albumId)
                    .add("albumUrl", albumUrl)
                    .toString();
        }
    }

    public record ExternalLink(
            @NotBlank
            String title,
            @NotBlank
            String url
    ) {

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("title", title)
                    .add("url", url)
                    .toString();
        }
    }
}
