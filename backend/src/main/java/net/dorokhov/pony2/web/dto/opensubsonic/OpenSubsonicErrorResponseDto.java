package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicErrorResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicErrorResponseDto> {

    public OpenSubsonicErrorResponseDto(OpenSubsonicResponseDto.AbstractResponse.Error error) {
        setError(error);
    }
}
