package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;

import jakarta.annotation.Nullable;

public final class AlbumDto extends BaseDto<AlbumDto> {

    private String name;
    private Integer year;
    private String artworkId;
    private String artistId;

    public @Nullable String getName() {
        return name;
    }

    public AlbumDto setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    public @Nullable Integer getYear() {
        return year;
    }

    public AlbumDto setYear(@Nullable Integer year) {
        this.year = year;
        return this;
    }

    public @Nullable String getArtworkId() {
        return artworkId;
    }

    public AlbumDto setArtworkId(@Nullable String artworkId) {
        this.artworkId = artworkId;
        return this;
    }

    public String getArtistId() {
        return artistId;
    }

    public AlbumDto setArtistId(String artistId) {
        this.artistId = artistId;
        return this;
    }

    public static AlbumDto of(Album album) {
        return new AlbumDto()
                .setId(album.getId())
                .setCreationDate(album.getCreationDate())
                .setUpdateDate(album.getUpdateDate())
                .setName(album.getName())
                .setYear(album.getYear())
                .setArtworkId(album.getArtwork() != null ? album.getArtwork().getId() : null)
                .setArtistId(album.getArtist().getId());
    }
}
