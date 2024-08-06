package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Artist;

public final class ArtistDto extends BaseDto<ArtistDto> {

    private String name;
    private String artworkId;

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

    public static ArtistDto of(Artist artist) {
        return new ArtistDto()
                .setId(artist.getId())
                .setCreationDate(artist.getCreationDate())
                .setUpdateDate(artist.getUpdateDate())
                .setName(artist.getName())
                .setArtworkId(artist.getArtwork() != null ? artist.getArtwork().getId() : null);
    }
}
