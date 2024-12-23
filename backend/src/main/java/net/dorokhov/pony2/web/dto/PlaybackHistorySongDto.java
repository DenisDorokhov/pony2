package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.PlaybackHistorySong;

import java.time.LocalDateTime;

public class PlaybackHistorySongDto {

    private String id;
    private LocalDateTime creationDate;
    private SongDto song;

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

    public SongDto getSong() {
        return song;
    }

    public PlaybackHistorySongDto setSong(SongDto song) {
        this.song = song;
        return this;
    }

    public static PlaybackHistorySongDto of(PlaybackHistorySong playlistSong, boolean isAdmin) {
        return new PlaybackHistorySongDto()
                .setId(playlistSong.getId())
                .setCreationDate(playlistSong.getCreationDate())
                .setSong(SongDto.of(playlistSong.getSong(), isAdmin));
    }
}
