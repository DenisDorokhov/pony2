package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.service.PlaylistService;

import java.util.List;

public class RestoredPlaylistsDto {

    public static class UserPlaylist {

        private String userEmail;
        private PlaylistDto playlist;

        public String getUserEmail() {
            return userEmail;
        }

        public UserPlaylist setUserEmail(String userEmail) {
            this.userEmail = userEmail;
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
    private List<String> notFoundUserEmails;
    private List<String> notFoundSongPaths;

    public List<UserPlaylist> getUserPlaylists() {
        return userPlaylists;
    }

    public RestoredPlaylistsDto setUserPlaylists(List<UserPlaylist> userPlaylists) {
        this.userPlaylists = userPlaylists;
        return this;
    }

    public List<String> getNotFoundUserEmails() {
        return notFoundUserEmails;
    }

    public RestoredPlaylistsDto setNotFoundUserEmails(List<String> notFoundUserEmails) {
        this.notFoundUserEmails = notFoundUserEmails;
        return this;
    }

    public List<String> getNotFoundSongPaths() {
        return notFoundSongPaths;
    }

    public RestoredPlaylistsDto setNotFoundSongPaths(List<String> notFoundSongPaths) {
        this.notFoundSongPaths = notFoundSongPaths;
        return this;
    }

    public static RestoredPlaylistsDto of(PlaylistService.RestoredPlaylists restoredPlaylists) {
        return new RestoredPlaylistsDto()
                .setUserPlaylists(restoredPlaylists.playlists().stream()
                        .map(playlist -> new UserPlaylist()
                                .setUserEmail(playlist.getUser().getEmail())
                                .setPlaylist(PlaylistDto.of(playlist)))
                        .toList())
                .setNotFoundUserEmails(restoredPlaylists.notFoundUserEmails())
                .setNotFoundSongPaths(restoredPlaylists.notFoundSongPaths());
    }
}
