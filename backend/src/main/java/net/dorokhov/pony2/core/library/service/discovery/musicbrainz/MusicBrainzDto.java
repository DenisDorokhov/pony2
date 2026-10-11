package net.dorokhov.pony2.core.library.service.discovery.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class MusicBrainzDto {

    private MusicBrainzDto() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseGroupSearch(
            int count,
            int offset,
            @JsonProperty("release-groups") List<ReleaseGroup> releaseGroups
    ) {
        public ReleaseGroupSearch {
            releaseGroups = List.copyOf(requireNonNull(releaseGroups));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseGroup(
            String id,
            @Nullable String title,
            @Nullable @JsonProperty("primary-type") String primaryType,
            @JsonProperty("secondary-types") List<String> secondaryTypes,
            @Nullable @JsonProperty("first-release-date") String firstReleaseDate,
            @JsonProperty("artist-credit") List<ArtistCredit> artistCredit,
            List<Release> releases
    ) {
        public ReleaseGroup {
            requireNonNull(id);
            secondaryTypes = secondaryTypes != null ? List.copyOf(secondaryTypes) : List.of();
            artistCredit = artistCredit != null ? List.copyOf(artistCredit) : List.of();
            releases = releases != null ? List.copyOf(releases) : List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Release(String id, @Nullable String title) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistCredit(@Nullable String name, @Nullable String joinphrase, Artist artist) {
        public ArtistCredit {
            requireNonNull(artist);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artist(String id, @Nullable String name, @Nullable Area area) {
        public Artist {
            requireNonNull(id);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Area(@JsonProperty("iso-3166-1-codes") List<String> country) {
        public Area {
            country = country != null ? List.copyOf(country) : List.of();
        }
    }
}
