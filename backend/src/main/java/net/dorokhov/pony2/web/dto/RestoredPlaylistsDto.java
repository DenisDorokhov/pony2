package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.service.PlaylistService;

import java.util.List;

public class RestoredPlaylistsDto {

    public static class UserPlaylist {

        private String userId;
        private PlaylistDto playlist;

        public String getUserId() {
            return userId;
        }

        public UserPlaylist setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public PlaylistDto getPlaylist() {
            return playlist;
        }

        public UserPlaylist setPlaylist(PlaylistDto playlist) {
            this.playlist = playlist;
            return this;
        }
    }

    private List<UserPlaylist> userPlaylists;
    private List<String> notFoundSongs;

    public List<UserPlaylist> getUserPlaylists() {
        return userPlaylists;
    }

    public RestoredPlaylistsDto setUserPlaylists(List<UserPlaylist> userPlaylists) {
        this.userPlaylists = userPlaylists;
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
                .setUserPlaylists(restoredPlaylists.playlists().stream()
                        .map(playlist -> new UserPlaylist()
                                .setUserId(playlist.getUser().getId())
                                .setPlaylist(PlaylistDto.of(playlist)))
                        .toList())
                .setNotFoundSongs(restoredPlaylists.notFoundSongs());
    }
}
