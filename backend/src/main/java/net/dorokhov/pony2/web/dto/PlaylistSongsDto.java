package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Playlist;

import java.util.List;

public class PlaylistSongsDto {

    private PlaylistDto playlist;
    private List<PlaylistSongDto> songs;

    public PlaylistDto getPlaylist() {
        return playlist;
    }

    public PlaylistSongsDto setPlaylist(PlaylistDto playlist) {
        this.playlist = playlist;
        return this;
    }

    public List<PlaylistSongDto> getSongs() {
        return songs;
    }

    public PlaylistSongsDto setSongs(List<PlaylistSongDto> songs) {
        this.songs = songs;
        return this;
    }

    public static PlaylistSongsDto of(Playlist playlist, boolean isAdmin) {
        return new PlaylistSongsDto()
                .setPlaylist(PlaylistDto.of(playlist))
                .setSongs(playlist.getSongs().stream()
                        .map(playlistSong -> PlaylistSongDto.of(playlistSong, isAdmin))
                        .toList());
    }
}
