package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicAlbumID3WithSongs;

public class OpenSubsonicAlbumResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicAlbumResponseDto> {

    private OpenSubsonicAlbumID3WithSongs album;

    public OpenSubsonicAlbumID3WithSongs getAlbum() {
        return album;
    }

    public OpenSubsonicAlbumResponseDto setAlbum(OpenSubsonicAlbumID3WithSongs album) {
        this.album = album;
        return this;
    }
}
