package net.dorokhov.pony3.core.library.service;

import net.dorokhov.pony3.api.config.service.ConfigService;
import net.dorokhov.pony3.api.installation.service.InstallationService;
import net.dorokhov.pony3.api.library.domain.ScanJob;
import net.dorokhov.pony3.api.library.service.ScanJobService;
import net.dorokhov.pony3.api.library.service.exception.ConcurrentScanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class ScanJobScheduler {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final InstallationService installationService;
    private final ConfigService configService;
    private final ScanJobService scanJobService;

    public ScanJobScheduler(
            InstallationService installationService,
            ConfigService configService,
            ScanJobService scanJobService
    ) {
        this.installationService = installationService;
        this.configService = configService;
        this.scanJobService = scanJobService;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    @Transactional
    public Optional<ScanJob> startAutoScanJobIfNeeded() {

        logger.trace("Checking if automatic scan needed...");
        boolean shouldScan = false;

        if (installationService.getInstallation().isPresent()) {
            Integer autoScanInterval = configService.getAutoScanInterval().orElse(null);
            if (autoScanInterval != null) {
                shouldScan = shouldAutoScanByInterval(autoScanInterval);
            } else {
                logger.trace("Automatic scan is off.");
            }
        } else {
            logger.trace("Not installed, automatic scan is not started.");
        }

        if (shouldScan) {
            logger.info("Starting automatic scan...");
            try {
                return Optional.of(scanJobService.startScanJob());
            } catch (ConcurrentScanException e) {
                logger.info("Scan job is already running, automatic scan is not started.");
            }
        }
        return Optional.empty();
    }

    private boolean shouldAutoScanByInterval(int autoScanInterval) {
        Page<ScanJob> page = scanJobService.getAll(PageRequest.of(0, 1, Sort.Direction.DESC, "creationDate", "updateDate"));
        ScanJob lastJob = page.getTotalElements() > 0 ? page.getContent().getFirst() : null;
        if (lastJob != null) {
            LocalDateTime jobDate = lastJob.getUpdateDate();
            if (jobDate == null) {
                jobDate = lastJob.getCreationDate();
            }
            long secondsSinceLastScan = jobDate.until(LocalDateTime.now(), ChronoUnit.SECONDS);
            if (secondsSinceLastScan >= autoScanInterval) {
                return true;
            } else {
                logger.trace("Too early for automatic scan.");
            }
        } else {
            logger.trace("Library was never scanned before.");
            return true;
        }
        return false;
    }
}
