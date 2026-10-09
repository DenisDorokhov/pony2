package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dorokhov.pony2.api.library.domain.ScanJob;
import net.dorokhov.pony2.api.library.domain.ScanJobProgress;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.api.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryScanJobObserver implements ScanJobService.Observer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ScanJobService scanJobService;
    private final DiscoveryJobService discoveryJobService;
    private final LogService logService;

    public DiscoveryScanJobObserver(
            ScanJobService scanJobService,
            DiscoveryJobService discoveryJobService,
            LogService logService
    ) {
        this.scanJobService = scanJobService;
        this.discoveryJobService = discoveryJobService;
        this.logService = logService;
    }

    @PostConstruct
    public void subscribe() {
        scanJobService.addObserver(this);
    }

    @PreDestroy
    public void unsubscribe() {
        scanJobService.removeObserver(this);
    }

    @Override
    public void onScanJobStarting(ScanJob scanJob) {
    }

    @Override
    public void onScanJobStarted(ScanJob scanJob) {
    }

    @Override
    public void onScanJobProgress(ScanJobProgress scanJobProgress) {
    }

    @Override
    public void onScanJobCompleting(ScanJob scanJob) {
    }

    @Override
    public void onScanJobCompleted(ScanJob scanJob) {
        try {
            discoveryJobService.startFullJob();
        } catch (ConcurrentLibraryJobException e) {
            logService.warn(logger, "Could not start full discovery job after scan job.", e);
        }
    }

    @Override
    public void onScanJobInterrupting(ScanJob scanJob) {
    }

    @Override
    public void onScanJobInterrupted(ScanJob scanJob) {
    }

    @Override
    public void onScanJobFailing(ScanJob scanJob) {
    }

    @Override
    public void onScanJobFailed(ScanJob scanJob) {
    }
}
