package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicChild;

public class OpenSubsonicSongResponse extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicSongResponse> {

    private OpenSubsonicChild child;

    public OpenSubsonicChild getChild() {
        return child;
    }

    public OpenSubsonicSongResponse setChild(OpenSubsonicChild child) {
        this.child = child;
        return this;
    }
}
