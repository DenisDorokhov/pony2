package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicTokenInfoResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicTokenInfoResponseDto> {

    private TokenInfo tokenInfo;

    public OpenSubsonicTokenInfoResponseDto(TokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
    }

    public TokenInfo getTokenInfo() {
        return tokenInfo;
    }

    public OpenSubsonicTokenInfoResponseDto setTokenInfo(TokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
        return this;
    }

    public static class TokenInfo {

        private String username;

        public String getUsername() {
            return username;
        }

        public TokenInfo setUsername(String username) {
            this.username = username;
            return this;
        }
    }
}
