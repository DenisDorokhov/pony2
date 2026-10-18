package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Playlist;

import java.util.List;
import java.util.function.Function;

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

    public static PlaylistSongsDto of(Playlist playlist, Function<Album, AlbumDetailsDto> albumMapper, boolean isAdmin) {
        return new PlaylistSongsDto()
                .setPlaylist(PlaylistDto.of(playlist))
                .setSongs(playlist.getSongs().stream()
                        .map(playlistSong -> PlaylistSongDto.of(playlistSong, albumMapper, isAdmin))
                        .toList());
    }
}
