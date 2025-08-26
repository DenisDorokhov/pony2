package net.dorokhov.pony2.web.dto.opensubsonic;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OpenSubsonicLicenseResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicLicenseResponseDto> {

    private License license;

    public OpenSubsonicLicenseResponseDto(License license) {
        this.license = license;
    }

    public License getLicense() {
        return license;
    }

    public OpenSubsonicLicenseResponseDto setLicense(License license) {
        this.license = license;
        return this;
    }

    public static class License {

        private boolean valid = true;
        private String email;
        private String licenseExpires;
        private String trialExpires;

        public boolean isValid() {
            return valid;
        }

        public License setValid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public String getEmail() {
            return email;
        }

        public License setEmail(String email) {
            this.email = email;
            return this;
        }

        public String getLicenseExpires() {
            return licenseExpires;
        }

        public License setLicenseExpires(String licenseExpires) {
            this.licenseExpires = licenseExpires;
            return this;
        }

        public String getTrialExpires() {
            return trialExpires;
        }

        public License setTrialExpires(String trialExpires) {
            this.trialExpires = trialExpires;
            return this;
        }
    }
}
