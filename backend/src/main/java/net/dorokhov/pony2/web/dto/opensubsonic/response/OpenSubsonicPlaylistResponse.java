package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicPlaylistWithSongs;

public class OpenSubsonicPlaylistResponse extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicPlaylistResponse> {

    private OpenSubsonicPlaylistWithSongs playlist;

    public OpenSubsonicPlaylistWithSongs getPlaylist() {
        return playlist;
    }

    public OpenSubsonicPlaylistResponse setPlaylist(OpenSubsonicPlaylistWithSongs playlist) {
        this.playlist = playlist;
        return this;
    }
}
