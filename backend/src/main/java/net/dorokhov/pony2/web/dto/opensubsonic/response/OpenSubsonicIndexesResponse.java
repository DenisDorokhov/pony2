package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicIndexes;

public class OpenSubsonicIndexesResponse extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicIndexesResponse> {

    private OpenSubsonicIndexes indexes;

    public OpenSubsonicIndexes getIndexes() {
        return indexes;
    }

    public OpenSubsonicIndexesResponse setIndexes(OpenSubsonicIndexes indexes) {
        this.indexes = indexes;
        return this;
    }
}
