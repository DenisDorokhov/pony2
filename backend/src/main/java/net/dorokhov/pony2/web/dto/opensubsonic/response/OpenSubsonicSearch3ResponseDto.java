package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicSearchResult3;

public class OpenSubsonicSearch3ResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicSearch3ResponseDto> {

    private OpenSubsonicSearchResult3 searchResult3;

    public OpenSubsonicSearchResult3 getSearchResult3() {
        return searchResult3;
    }

    public OpenSubsonicSearch3ResponseDto setSearchResult3(OpenSubsonicSearchResult3 searchResult3) {
        this.searchResult3 = searchResult3;
        return this;
    }
}
