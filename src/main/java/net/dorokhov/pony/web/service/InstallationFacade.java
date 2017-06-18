package net.dorokhov.pony.web.service;

import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.domain.InstallationStatusDto;
import net.dorokhov.pony.web.service.exception.InvalidInstallationSecretException;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;

public interface InstallationFacade {
    
    InstallationStatusDto getInstallationStatus();

    InstallationDto getInstallation() throws ObjectNotFoundException;

    boolean verifyInstallationSecret(String installationSecret);

    InstallationDto install(InstallationCommandDto command) throws InvalidInstallationSecretException, AlreadyInstalledException;
}
