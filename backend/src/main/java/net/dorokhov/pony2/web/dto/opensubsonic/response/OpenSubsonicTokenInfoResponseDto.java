package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicTokenInfo;

public class OpenSubsonicTokenInfoResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicTokenInfoResponseDto> {

    private OpenSubsonicTokenInfo tokenInfo;

    public OpenSubsonicTokenInfoResponseDto(OpenSubsonicTokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
    }

    public OpenSubsonicTokenInfo getTokenInfo() {
        return tokenInfo;
    }

    public OpenSubsonicTokenInfoResponseDto setTokenInfo(OpenSubsonicTokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
        return this;
    }

}
