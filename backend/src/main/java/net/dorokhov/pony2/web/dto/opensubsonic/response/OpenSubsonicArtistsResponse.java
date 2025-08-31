package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicArtistsID3;

public class OpenSubsonicArtistsResponse extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicArtistsResponse> {

    private OpenSubsonicArtistsID3 artists;

    public OpenSubsonicArtistsID3 getArtists() {
        return artists;
    }

    public OpenSubsonicArtistsResponse setArtists(OpenSubsonicArtistsID3 artists) {
        this.artists = artists;
        return this;
    }
}
