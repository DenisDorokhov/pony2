package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.PlaybackHistorySong;

import java.time.LocalDateTime;

public class PlaybackHistorySongDto {

    private String id;
    private LocalDateTime creationDate;
    private SongDetailsDto song;

    public String getId() {
        return id;
    }

    public PlaybackHistorySongDto setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public PlaybackHistorySongDto setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public SongDetailsDto getSong() {
        return song;
    }

    public PlaybackHistorySongDto setSong(SongDetailsDto song) {
        this.song = song;
        return this;
    }

    public static PlaybackHistorySongDto of(PlaybackHistorySong playlistSong, boolean isAdmin) {
        return new PlaybackHistorySongDto()
                .setId(playlistSong.getId())
                .setCreationDate(playlistSong.getCreationDate())
                .setSong(SongDetailsDto.of(playlistSong.getSong(), isAdmin));
    }
}
