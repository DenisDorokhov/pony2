package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicLicense;

public class OpenSubsonicLicenseResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicLicenseResponseDto> {

    private OpenSubsonicLicense license;

    public OpenSubsonicLicenseResponseDto(OpenSubsonicLicense license) {
        this.license = license;
    }

    public OpenSubsonicLicense getLicense() {
        return license;
    }

    public OpenSubsonicLicenseResponseDto setLicense(OpenSubsonicLicense license) {
        this.license = license;
        return this;
    }

}
