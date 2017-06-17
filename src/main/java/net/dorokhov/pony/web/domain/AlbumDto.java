package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.library.domain.Album;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AlbumDto extends BaseDto {

    private final String name;
    private final Integer year;
    private final Long artwork;
    private final Long artist;

    public AlbumDto(Long id, LocalDateTime creationDate, LocalDateTime updateDate,
                    String name, Integer year, Long artwork, Long artist) {
        super(id, creationDate, updateDate);
        this.name = name;
        this.year = year;
        this.artwork = artwork;
        this.artist = checkNotNull(artist);
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Integer getYear() {
        return year;
    }

    @Nullable
    public Long getArtwork() {
        return artwork;
    }

    public Long getArtist() {
        return artist;
    }

    public static AlbumDto of(Album album) {
        return new AlbumDto(album.getId(), album.getCreationDate(), album.getUpdateDate(),
                album.getName(), album.getYear(),
                album.getArtwork() != null ? album.getArtwork().getId() : null,
                album.getArtist().getId());
    }
}
