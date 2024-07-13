package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.web.service.exception.SecretNotFoundException;
import net.dorokhov.pony3.api.installation.domain.Installation;
import net.dorokhov.pony3.api.installation.service.InstallationService;
import net.dorokhov.pony3.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony3.web.dto.InstallationCommandDto;
import net.dorokhov.pony3.web.dto.InstallationDto;
import net.dorokhov.pony3.web.dto.InstallationStatusDto;
import net.dorokhov.pony3.web.service.exception.InvalidInstallationSecretException;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Service
public class InstallationFacade {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InstallationService installationService;
    private final InstallationSecretService installationSecretService;

    public InstallationFacade(
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

    @Transactional(readOnly = true)
    public InstallationStatusDto getInstallationStatus() {
        return new InstallationStatusDto()
                .setInstalled(installationService.getInstallation().isPresent());
    }

    @Transactional(readOnly = true)
    public InstallationDto getInstallation() throws ObjectNotFoundException {
        Installation installation = installationService.getInstallation().orElse(null);
        if (installation == null) {
            throw new ObjectNotFoundException(Installation.class);
        }
        return InstallationDto.of(installation);
    }

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

    @Transactional
    public InstallationDto install(InstallationCommandDto command) throws InvalidInstallationSecretException, AlreadyInstalledException {
        if (!verifyInstallationSecret(command.getInstallationSecret())) {
            throw new InvalidInstallationSecretException();
        }
        return InstallationDto.of(installationService.install(command.convert()));
    }
}
