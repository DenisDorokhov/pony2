package net.dorokhov.pony.web.validation;

import net.dorokhov.pony.web.service.InstallationServiceFacade;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public final class InstallationSecretValidator implements ConstraintValidator<InstallationSecret, String> {
    
    private final InstallationServiceFacade installationServiceFacade;

    public InstallationSecretValidator(InstallationServiceFacade installationServiceFacade) {
        this.installationServiceFacade = installationServiceFacade;
    }

    @Override
    public void initialize(InstallationSecret annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return installationServiceFacade.verifyInstallationSecret(value);
    }
}
