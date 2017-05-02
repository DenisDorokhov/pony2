package net.dorokhov.pony.library.service.impl;

import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.library.service.ScanJobService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScanJobScheduler {
    
    private final InstallationService installationService;
    private final ScanJobService scanJobService;

    public ScanJobScheduler(InstallationService installationService, ScanJobService scanJobService) {
        this.installationService = installationService;
        this.scanJobService = scanJobService;
    }

    @Transactional
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void startAutoScanJob() {
        if (installationService.getInstallation() != null) {
            scanJobService.startAutoScanJobIfNeeded();
        }
    }
}
