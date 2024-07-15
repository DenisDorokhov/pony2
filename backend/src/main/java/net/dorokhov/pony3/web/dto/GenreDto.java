package net.dorokhov.pony3.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony3.api.library.domain.Genre;

public final class GenreDto extends BaseDto<GenreDto> {

    private String name;
    private String artworkId;

    @Nullable
    public String getName() {
        return name;
    }

    public GenreDto setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getArtworkId() {
        return artworkId;
    }

    public GenreDto setArtworkId(@Nullable String artworkId) {
        this.artworkId = artworkId;
        return this;
    }

    public static GenreDto of(Genre genre) {
        return new GenreDto()
                .setId(genre.getId())
                .setCreationDate(genre.getCreationDate())
                .setUpdateDate(genre.getUpdateDate())
                .setName(genre.getName())
                .setArtworkId(genre.getArtwork() != null ? genre.getArtwork().getId() : null);
    }
}
