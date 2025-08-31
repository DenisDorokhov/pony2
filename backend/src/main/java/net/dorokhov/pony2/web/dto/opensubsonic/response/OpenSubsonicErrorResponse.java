package net.dorokhov.pony2.web.dto.opensubsonic.response;

public class OpenSubsonicErrorResponse extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicErrorResponse> {

    public OpenSubsonicErrorResponse(OpenSubsonicResponse.AbstractResponse.Error error) {
        setError(error);
    }
}
