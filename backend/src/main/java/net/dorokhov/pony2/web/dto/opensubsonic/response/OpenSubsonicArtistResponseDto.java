package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicArtistWithAlbumsID3;

public class OpenSubsonicArtistResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicArtistResponseDto> {

    private OpenSubsonicArtistWithAlbumsID3 artist;

    public OpenSubsonicArtistWithAlbumsID3 getArtist() {
        return artist;
    }

    public OpenSubsonicArtistResponseDto setArtist(OpenSubsonicArtistWithAlbumsID3 artist) {
        this.artist = artist;
        return this;
    }
}
