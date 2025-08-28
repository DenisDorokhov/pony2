package net.dorokhov.pony2.web.dto.opensubsonic.response;

import java.util.ArrayList;
import java.util.List;

public class OpenSubsonicExtensionsResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicExtensionsResponseDto> {

    private List<?> openSubsonicExtensions = new ArrayList<>();

    public List<?> getOpenSubsonicExtensions() {
        return openSubsonicExtensions;
    }

    public OpenSubsonicExtensionsResponseDto setOpenSubsonicExtensions(List<?> openSubsonicExtensions) {
        this.openSubsonicExtensions = openSubsonicExtensions;
        return this;
    }
}
