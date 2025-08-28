package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicPlaylistWithSongs;

public class OpenSubsonicPlaylistResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicPlaylistResponseDto> {

    private OpenSubsonicPlaylistWithSongs playlist;

    public OpenSubsonicPlaylistWithSongs getPlaylist() {
        return playlist;
    }

    public OpenSubsonicPlaylistResponseDto setPlaylist(OpenSubsonicPlaylistWithSongs playlist) {
        this.playlist = playlist;
        return this;
    }
}
