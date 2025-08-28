package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicStarred2;

public class OpenSubsonicStarred2ResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicStarred2ResponseDto> {

    private OpenSubsonicStarred2 starred2;

    public OpenSubsonicStarred2 getStarred2() {
        return starred2;
    }

    public OpenSubsonicStarred2ResponseDto setStarred2(OpenSubsonicStarred2 starred2) {
        this.starred2 = starred2;
        return this;
    }
}
