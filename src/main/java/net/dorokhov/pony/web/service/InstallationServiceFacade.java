package net.dorokhov.pony.web.service;

import net.dorokhov.pony.common.SecretNotFoundException;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
public class InstallationServiceFacade {
    
    private final InstallationService installationService;
    private final InstallationSecretManager installationSecretManager;

    public InstallationServiceFacade(InstallationService installationService, 
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

    public InstallationDto getInstallation() {
        Installation installation = installationService.getInstallation();
        return installation != null ? new InstallationDto(installation) : null;
    }
    
    public InstallationDto install(InstallationCommandDto command) throws AlreadyInstalledException {
        return new InstallationDto(installationService.install(command.convert()));
    }
}
