package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.library.domain.Genre;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

public final class GenreDto extends BaseDto {

    private final String name;
    private final Long artwork;

    public GenreDto(Long id, LocalDateTime creationDate, LocalDateTime updateDate, String name, Long artwork) {
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
