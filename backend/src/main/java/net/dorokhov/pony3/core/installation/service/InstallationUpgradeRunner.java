package net.dorokhov.pony3.core.installation.service;

import net.dorokhov.pony3.api.installation.service.InstallationService;
import net.dorokhov.pony3.api.installation.service.exception.NotInstalledException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InstallationUpgradeRunner implements ApplicationRunner {
    
    private final InstallationService installationService;

    public InstallationUpgradeRunner(InstallationService installationService) {
        this.installationService = installationService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws NotInstalledException {
        if (installationService.getInstallation().isPresent()) {
            installationService.upgradeIfNeeded();
        }
    }
}
