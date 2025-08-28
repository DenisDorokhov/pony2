package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicChild;

public class OpenSubsonicSongResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicSongResponseDto> {

    private OpenSubsonicChild child;

    public OpenSubsonicChild getChild() {
        return child;
    }

    public OpenSubsonicSongResponseDto setChild(OpenSubsonicChild child) {
        this.child = child;
        return this;
    }
}
