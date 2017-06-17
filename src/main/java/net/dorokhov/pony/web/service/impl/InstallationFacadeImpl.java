package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.common.SecretNotFoundException;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.service.InstallationFacade;
import net.dorokhov.pony.web.service.exception.InvalidInstallationSecretException;
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
    private final InstallationSecretManager installationSecretManager;

    public InstallationFacadeImpl(InstallationService installationService,
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
    @Transactional(readOnly = true)
    public InstallationDto getInstallation() {
        Installation installation = installationService.getInstallation();
        return installation != null ? InstallationDto.of(installation) : null;
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
    @Transactional
    public InstallationDto install(InstallationCommandDto command) throws InvalidInstallationSecretException, AlreadyInstalledException {
        if (!verifyInstallationSecret(command.getInstallationSecret())) {
            throw new InvalidInstallationSecretException();
        }
        return InstallationDto.of(installationService.install(command.convert()));
    }
}
