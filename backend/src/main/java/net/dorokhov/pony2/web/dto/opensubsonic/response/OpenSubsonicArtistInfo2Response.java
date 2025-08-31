package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicArtistInfo2;

public class OpenSubsonicArtistInfo2Response extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicArtistInfo2Response> {

    private OpenSubsonicArtistInfo2 artistInfo2;

    public OpenSubsonicArtistInfo2 getArtistInfo2() {
        return artistInfo2;
    }

    public OpenSubsonicArtistInfo2Response setArtistInfo2(OpenSubsonicArtistInfo2 artistInfo2) {
        this.artistInfo2 = artistInfo2;
        return this;
    }
}
