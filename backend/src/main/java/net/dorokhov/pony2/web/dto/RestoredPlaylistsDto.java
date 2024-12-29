package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.service.PlaylistService;

import java.util.List;

public class RestoredPlaylistsDto {

    private List<PlaylistDto> playlists;
    private List<String> notFoundSongs;

    public List<PlaylistDto> getPlaylists() {
        return playlists;
    }

    public RestoredPlaylistsDto setPlaylists(List<PlaylistDto> playlists) {
        this.playlists = playlists;
        return this;
    }

    public List<String> getNotFoundSongs() {
        return notFoundSongs;
    }

    public RestoredPlaylistsDto setNotFoundSongs(List<String> notFoundSongs) {
        this.notFoundSongs = notFoundSongs;
        return this;
    }

    public static RestoredPlaylistsDto of(PlaylistService.RestoredPlaylists restoredPlaylists) {
        return new RestoredPlaylistsDto()
                .setPlaylists(restoredPlaylists.playlists().stream()
                        .map(PlaylistDto::of)
                        .toList())
                .setNotFoundSongs(restoredPlaylists.notFoundSongs());
    }
}
