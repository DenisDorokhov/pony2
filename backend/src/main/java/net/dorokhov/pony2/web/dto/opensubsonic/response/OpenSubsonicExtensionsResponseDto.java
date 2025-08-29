package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicExtension;

import java.util.ArrayList;
import java.util.List;

public class OpenSubsonicExtensionsResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicExtensionsResponseDto> {

    private List<OpenSubsonicExtension> openSubsonicExtensions = new ArrayList<>();

    public List<OpenSubsonicExtension> getOpenSubsonicExtensions() {
        return openSubsonicExtensions;
    }

    public OpenSubsonicExtensionsResponseDto setOpenSubsonicExtensions(List<OpenSubsonicExtension> openSubsonicExtensions) {
        this.openSubsonicExtensions = openSubsonicExtensions;
        return this;
    }
}
