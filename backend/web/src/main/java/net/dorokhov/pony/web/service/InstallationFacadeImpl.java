package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import net.dorokhov.pony.api.installation.domain.Installation;
import net.dorokhov.pony.api.installation.service.InstallationService;
import net.dorokhov.pony.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.domain.InstallationStatusDto;
import net.dorokhov.pony.web.service.exception.InvalidInstallationSecretException;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
public class InstallationFacadeImpl implements InstallationFacade {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InstallationService installationService;
    private final InstallationSecretService installationSecretService;

    public InstallationFacadeImpl(
            InstallationService installationService,
            InstallationSecretService installationSecretService
    ) {
        this.installationService = installationService;
        this.installationSecretService = installationSecretService;
    }

    @PostConstruct
    public void assureInstallationSecretExists() throws IOException {
        try {
            installationSecretService.fetchInstallationSecret();
        } catch (SecretNotFoundException e) {
            installationSecretService.generateAndStoreInstallationSecret();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InstallationStatusDto getInstallationStatus() {
        return new InstallationStatusDto(installationService.getInstallation() != null);
    }

    @Override
    @Transactional(readOnly = true)
    public InstallationDto getInstallation() throws ObjectNotFoundException {
        Installation installation = installationService.getInstallation();
        if (installation == null) {
            throw new ObjectNotFoundException(Installation.class);
        }
        return InstallationDto.of(installation);
    }

    @Override
    public boolean verifyInstallationSecret(String installationSecret) {
        try {
            return installationSecretService.fetchInstallationSecret().equals(installationSecret);
        } catch (SecretNotFoundException e) {
            logger.error("Could not find installation secret for verification.");
        } catch (IOException e) {
            logger.error("Could not fetch installation secret for verification.", e);
        }
        return false;
    }

    @Override
    @Transactional
    public InstallationDto install(InstallationCommandDto command) throws InvalidInstallationSecretException, AlreadyInstalledException {
        if (!verifyInstallationSecret(command.getInstallationSecret())) {
            throw new InvalidInstallationSecretException();
        }
        return InstallationDto.of(installationService.install(command.convert()));
    }
}
