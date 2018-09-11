package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.Album;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AlbumDto extends BaseDto {

    private final String name;
    private final Integer year;
    private final String artworkId;
    private final String artistId;

    private AlbumDto(String id, LocalDateTime creationDate, @Nullable LocalDateTime updateDate,
                     @Nullable String name, @Nullable Integer year, @Nullable String artworkId, String artistId) {
        super(id, creationDate, updateDate);
        this.name = name;
        this.year = year;
        this.artworkId = artworkId;
        this.artistId = checkNotNull(artistId);
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
    public String getArtworkId() {
        return artworkId;
    }

    public String getArtistId() {
        return artistId;
    }

    public static AlbumDto of(Album album) {
        return new AlbumDto(album.getId(), album.getCreationDate(), album.getUpdateDate(),
                album.getName(), album.getYear(),
                album.getArtwork() != null ? album.getArtwork().getId() : null,
                album.getArtist().getId());
    }
}
