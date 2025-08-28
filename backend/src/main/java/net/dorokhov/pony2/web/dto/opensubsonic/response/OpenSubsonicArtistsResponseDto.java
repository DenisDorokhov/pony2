package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicArtistsID3;

public class OpenSubsonicArtistsResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicArtistsResponseDto> {

    private OpenSubsonicArtistsID3 artists;

    public OpenSubsonicArtistsID3 getArtists() {
        return artists;
    }

    public OpenSubsonicArtistsResponseDto setArtists(OpenSubsonicArtistsID3 artists) {
        this.artists = artists;
        return this;
    }
}
