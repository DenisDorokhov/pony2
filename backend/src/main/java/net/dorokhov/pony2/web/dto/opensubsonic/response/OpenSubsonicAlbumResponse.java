package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicAlbumID3WithSongs;

public class OpenSubsonicAlbumResponse extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicAlbumResponse> {

    private OpenSubsonicAlbumID3WithSongs album;

    public OpenSubsonicAlbumID3WithSongs getAlbum() {
        return album;
    }

    public OpenSubsonicAlbumResponse setAlbum(OpenSubsonicAlbumID3WithSongs album) {
        this.album = album;
        return this;
    }
}
