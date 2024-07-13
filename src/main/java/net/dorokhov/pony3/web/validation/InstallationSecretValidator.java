package net.dorokhov.pony3.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.dorokhov.pony3.web.service.InstallationFacade;

public final class InstallationSecretValidator implements ConstraintValidator<InstallationSecret, String> {

    private final InstallationFacade installationFacade;

    public InstallationSecretValidator(InstallationFacade installationFacade) {
        this.installationFacade = installationFacade;
    }

    @Override
    public void initialize(InstallationSecret annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return installationFacade.verifyInstallationSecret(value);
    }
}
