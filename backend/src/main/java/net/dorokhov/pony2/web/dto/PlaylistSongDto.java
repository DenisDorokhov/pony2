package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.PlaylistSong;

import java.time.LocalDateTime;

public class PlaylistSongDto {

    private String id;
    private LocalDateTime creationDate;
    private SongDetailsDto song;

    public String getId() {
        return id;
    }

    public PlaylistSongDto setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public PlaylistSongDto setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public SongDetailsDto getSong() {
        return song;
    }

    public PlaylistSongDto setSong(SongDetailsDto song) {
        this.song = song;
        return this;
    }

    public static PlaylistSongDto of(PlaylistSong playlistSong, boolean isAdmin) {
        return new PlaylistSongDto()
                .setId(playlistSong.getId())
                .setCreationDate(playlistSong.getCreationDate())
                .setSong(SongDetailsDto.of(playlistSong.getSong(), isAdmin));
    }
}
