package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;

public final class DiscoveryJobDto extends BaseDto<DiscoveryJobDto> {

    private DiscoveryType discoveryType;
    private DiscoveryJob.Status status;
    private GenreDto genre;
    private ArtistDto artist;
    private AlbumDto album;
    private SongDto song;
    private LogMessageDto logMessage;

    public DiscoveryType getDiscoveryType() {
        return discoveryType;
    }

    public DiscoveryJobDto setDiscoveryType(DiscoveryType discoveryType) {
        this.discoveryType = discoveryType;
        return this;
    }

    public DiscoveryJob.Status getStatus() {
        return status;
    }

    public DiscoveryJobDto setStatus(DiscoveryJob.Status status) {
        this.status = status;
        return this;
    }

    @Nullable
    public GenreDto getGenre() {
        return genre;
    }

    public DiscoveryJobDto setGenre(@Nullable GenreDto genre) {
        this.genre = genre;
        return this;
    }

    @Nullable
    public ArtistDto getArtist() {
        return artist;
    }

    public DiscoveryJobDto setArtist(@Nullable ArtistDto artist) {
        this.artist = artist;
        return this;
    }

    @Nullable
    public AlbumDto getAlbum() {
        return album;
    }

    public DiscoveryJobDto setAlbum(@Nullable AlbumDto album) {
        this.album = album;
        return this;
    }

    @Nullable
    public SongDto getSong() {
        return song;
    }

    public DiscoveryJobDto setSong(@Nullable SongDto song) {
        this.song = song;
        return this;
    }

    @Nullable
    public LogMessageDto getLogMessage() {
        return logMessage;
    }

    public DiscoveryJobDto setLogMessage(@Nullable LogMessageDto logMessage) {
        this.logMessage = logMessage;
        return this;
    }

    public static DiscoveryJobDto of(
            DiscoveryJob discoveryJob,
            @Nullable GenreDto genre,
            @Nullable ArtistDto artist,
            @Nullable AlbumDto album,
            @Nullable SongDto song
    ) {
        return new DiscoveryJobDto()
                .setId(discoveryJob.getId())
                .setCreationDate(discoveryJob.getCreationDate())
                .setUpdateDate(discoveryJob.getUpdateDate())
                .setDiscoveryType(discoveryJob.getType())
                .setStatus(discoveryJob.getStatus())
                .setGenre(genre)
                .setArtist(artist)
                .setAlbum(album)
                .setSong(song)
                .setLogMessage(discoveryJob.getLogMessage() != null ? LogMessageDto.of(discoveryJob.getLogMessage()) : null);
    }
}
