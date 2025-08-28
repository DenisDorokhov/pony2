package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicPlaylist;

import java.util.List;

public class OpenSubsonicPlaylistsResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicPlaylistsResponseDto> {

    private Playlists playlists;

    public Playlists getPlaylists() {
        return playlists;
    }

    public OpenSubsonicPlaylistsResponseDto setPlaylists(Playlists playlists) {
        this.playlists = playlists;
        return this;
    }

    public static class Playlists {

        private List<OpenSubsonicPlaylist> playlist;

        public List<OpenSubsonicPlaylist> getPlaylist() {
            return playlist;
        }

        public Playlists setPlaylist(List<OpenSubsonicPlaylist> playlist) {
            this.playlist = playlist;
            return this;
        }
    }
}
