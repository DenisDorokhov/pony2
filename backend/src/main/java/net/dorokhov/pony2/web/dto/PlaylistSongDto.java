package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.PlaylistSong;

public class PlaylistSongDto {

    private String id;
    private SongDto song;

    public String getId() {
        return id;
    }

    public PlaylistSongDto setId(String id) {
        this.id = id;
        return this;
    }

    public SongDto getSong() {
        return song;
    }

    public PlaylistSongDto setSong(SongDto song) {
        this.song = song;
        return this;
    }

    public static PlaylistSongDto of(PlaylistSong playlistSong, boolean isAdmin) {
        return new PlaylistSongDto()
                .setId(playlistSong.getId())
                .setSong(SongDto.of(playlistSong.getSong(), isAdmin));
    }
}
