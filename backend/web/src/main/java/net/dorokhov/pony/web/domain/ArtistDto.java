package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Artist;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

public final class ArtistDto extends BaseDto {

    private final String name;
    private final Long artwork;

    ArtistDto(Long id, LocalDateTime creationDate, @Nullable LocalDateTime updateDate, 
              @Nullable String name, @Nullable Long artwork) {
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

    public static ArtistDto of(Artist artist) {
        return new ArtistDto(artist.getId(), artist.getCreationDate(), artist.getUpdateDate(),
                artist.getName(),
                artist.getArtwork() != null ? artist.getArtwork().getId() : null);
    }
}
