package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;

public final class ArtistDetailsDto {

    private ArtistDto artist;
    private ArtistDiscoveryDataDto discoveryData;

    public ArtistDto getArtist() {
        return artist;
    }

    public ArtistDetailsDto setArtist(ArtistDto artist) {
        this.artist = artist;
        return this;
    }

    public @Nullable ArtistDiscoveryDataDto getDiscoveryData() {
        return discoveryData;
    }

    public ArtistDetailsDto setDiscoveryData(@Nullable ArtistDiscoveryDataDto discoveryData) {
        this.discoveryData = discoveryData;
        return this;
    }

    public static ArtistDetailsDto of(Artist artist, @Nullable ArtistDiscoveryData discoveryData) {
        return new ArtistDetailsDto()
                .setArtist(ArtistDto.of(artist))
                .setDiscoveryData(discoveryData != null ? ArtistDiscoveryDataDto.of(discoveryData) : null);
    }
}
