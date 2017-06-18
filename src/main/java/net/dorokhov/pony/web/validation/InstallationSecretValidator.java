package net.dorokhov.pony.web.validation;

import net.dorokhov.pony.web.service.InstallationFacade;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
