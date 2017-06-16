package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.common.SecretNotFoundException;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.service.InstallationServiceFacade;
import net.dorokhov.pony.web.service.exception.InvalidInstallationSecretException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
public class InstallationServiceFacadeImpl implements InstallationServiceFacade {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InstallationService installationService;
    private final InstallationSecretManager installationSecretManager;

    public InstallationServiceFacadeImpl(InstallationService installationService,
                                         InstallationSecretManager installationSecretManager) {
        this.installationService = installationService;
        this.installationSecretManager = installationSecretManager;
    }

    @PostConstruct
    public void assureInstallationSecretExists() throws IOException {
        try {
            installationSecretManager.fetchInstallationSecret();
        } catch (SecretNotFoundException e) {
            installationSecretManager.generateAndStoreInstallationSecret();
        }
    }

    @Override
    public InstallationDto getInstallation() {
        Installation installation = installationService.getInstallation();
        return installation != null ? new InstallationDto(installation) : null;
    }

    @Override
    public boolean verifyInstallationSecret(String installationSecret) {
        try {
            return installationSecretManager.fetchInstallationSecret().equals(installationSecret);
        } catch (SecretNotFoundException e) {
            logger.error("Could not find installation secret for verification.");
        } catch (IOException e) {
            logger.error("Could not fetch installation secret for verification.", e);
        }
        return false;
    }

    @Override
    public InstallationDto install(InstallationCommandDto command) throws InvalidInstallationSecretException, AlreadyInstalledException {
        if (!verifyInstallationSecret(command.getInstallationSecret())) {
            throw new InvalidInstallationSecretException();
        }
        return new InstallationDto(installationService.install(command.convert()));
    }
}
