package net.dorokhov.pony.web.validation;

import net.dorokhov.pony.web.service.InstallationSecretManager;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public final class InstallationSecretValidator implements ConstraintValidator<InstallationSecret, String> {
    
    private final InstallationSecretManager installationSecretManager;

    public InstallationSecretValidator(InstallationSecretManager installationSecretManager) {
        this.installationSecretManager = installationSecretManager;
    }

    @Override
    public void initialize(InstallationSecret annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String installationSecret;
        try {
            installationSecret = installationSecretManager.fetchInstallationSecret();
        } catch (Exception e) {
            throw new RuntimeException("Could not fetch installation secret.", e);
        }
        return installationSecret.equals(value);
    }
}
