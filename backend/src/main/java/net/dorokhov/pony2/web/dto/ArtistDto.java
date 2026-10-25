package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;
import net.dorokhov.pony2.api.library.domain.ArtistGenre;

import java.util.List;

public final class ArtistDto extends BaseDto<ArtistDto> {

    private String name;
    private String artworkId;
    private List<GenreDto> genres;
    private ArtistDiscoveryDataDto discoveryData;

    @Nullable
    public String getName() {
        return name;
    }

    public ArtistDto setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getArtworkId() {
        return artworkId;
    }

    public ArtistDto setArtworkId(@Nullable String artworkId) {
        this.artworkId = artworkId;
        return this;
    }

    public List<GenreDto> getGenres() {
        return genres;
    }

    public ArtistDto setGenres(List<GenreDto> genres) {
        this.genres = genres;
        return this;
    }

    @Nullable
    public ArtistDiscoveryDataDto getDiscoveryData() {
        return discoveryData;
    }

    public ArtistDto setDiscoveryData(@Nullable ArtistDiscoveryDataDto discoveryData) {
        this.discoveryData = discoveryData;
        return this;
    }

    public static ArtistDto of(Artist artist, @Nullable ArtistDiscoveryData discoveryData) {
        return new ArtistDto()
                .setId(artist.getId())
                .setCreationDate(artist.getCreationDate())
                .setUpdateDate(artist.getUpdateDate())
                .setName(artist.getName())
                .setArtworkId(artist.getArtwork() != null ? artist.getArtwork().getId() : null)
                .setGenres(artist.getGenres().stream()
                        .sorted()
                        .map(ArtistGenre::getGenre)
                        .map(GenreDto::of)
                        .toList())
                .setDiscoveryData(discoveryData != null ? ArtistDiscoveryDataDto.of(discoveryData) : null);
    }
}
