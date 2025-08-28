package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicAlbumInfo;

public class OpenSubsonicAlbumInfo2ResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicAlbumInfo2ResponseDto> {

    private OpenSubsonicAlbumInfo albumInfo;

    public OpenSubsonicAlbumInfo getAlbumInfo() {
        return albumInfo;
    }

    public OpenSubsonicAlbumInfo2ResponseDto setAlbumInfo(OpenSubsonicAlbumInfo albumInfo) {
        this.albumInfo = albumInfo;
        return this;
    }
}
