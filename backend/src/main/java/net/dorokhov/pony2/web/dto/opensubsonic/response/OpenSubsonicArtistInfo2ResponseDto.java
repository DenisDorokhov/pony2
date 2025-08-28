package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicArtistInfo2;

public class OpenSubsonicArtistInfo2ResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicArtistInfo2ResponseDto> {

    private OpenSubsonicArtistInfo2 artistInfo2;

    public OpenSubsonicArtistInfo2 getArtistInfo2() {
        return artistInfo2;
    }

    public OpenSubsonicArtistInfo2ResponseDto setArtistInfo2(OpenSubsonicArtistInfo2 artistInfo2) {
        this.artistInfo2 = artistInfo2;
        return this;
    }
}
