package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicLicense {

    private boolean valid = true;
    private String email;
    private String licenseExpires;
    private String trialExpires;

    public boolean isValid() {
        return valid;
    }

    public OpenSubsonicLicense setValid(boolean valid) {
        this.valid = valid;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public OpenSubsonicLicense setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getLicenseExpires() {
        return licenseExpires;
    }

    public OpenSubsonicLicense setLicenseExpires(String licenseExpires) {
        this.licenseExpires = licenseExpires;
        return this;
    }

    public String getTrialExpires() {
        return trialExpires;
    }

    public OpenSubsonicLicense setTrialExpires(String trialExpires) {
        this.trialExpires = trialExpires;
        return this;
    }
}
