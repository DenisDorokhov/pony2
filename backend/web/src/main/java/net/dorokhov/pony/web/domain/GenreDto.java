package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Genre;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

public final class GenreDto extends BaseDto {

    private final String name;
    private final Long artwork;

    GenreDto(Long id, LocalDateTime creationDate, @Nullable LocalDateTime updateDate, @Nullable String name, @Nullable Long artwork) {
        super(id, creationDate, updateDate);
        this.name = name;
        this.artwork = artwork;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Long getArtwork() {
        return artwork;
    }

    public static GenreDto of(Genre genre) {
        return new GenreDto(genre.getId(), genre.getCreationDate(), genre.getUpdateDate(),
                genre.getName(),
                genre.getArtwork() != null ? genre.getArtwork().getId() : null);
    }
}
