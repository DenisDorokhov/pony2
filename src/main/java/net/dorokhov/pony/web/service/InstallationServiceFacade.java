package net.dorokhov.pony.web.service;

import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.service.exception.InvalidInstallationSecretException;

public interface InstallationServiceFacade {

    InstallationDto getInstallation();

    boolean verifyInstallationSecret(String installationSecret);

    InstallationDto install(InstallationCommandDto command) throws InvalidInstallationSecretException, AlreadyInstalledException;
}
