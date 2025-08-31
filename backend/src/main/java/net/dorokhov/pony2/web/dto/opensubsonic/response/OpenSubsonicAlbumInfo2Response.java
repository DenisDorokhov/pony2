package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicAlbumInfo;

public class OpenSubsonicAlbumInfo2Response extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicAlbumInfo2Response> {

    private OpenSubsonicAlbumInfo albumInfo;

    public OpenSubsonicAlbumInfo getAlbumInfo() {
        return albumInfo;
    }

    public OpenSubsonicAlbumInfo2Response setAlbumInfo(OpenSubsonicAlbumInfo albumInfo) {
        this.albumInfo = albumInfo;
        return this;
    }
}
