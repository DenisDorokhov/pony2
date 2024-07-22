package net.dorokhov.pony3.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony3.api.library.domain.ScanJob;
import net.dorokhov.pony3.api.log.service.LogService;
import net.dorokhov.pony3.core.library.repository.ScanJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScanJobInterruptionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final ScanJobRepository scanJobRepository;
    private final LogService logService;

    public ScanJobInterruptionService(
            ScanJobRepository scanJobRepository,
            LogService logService
    ) {
        this.scanJobRepository = scanJobRepository;
        this.logService = logService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void markCurrentJobsAsInterrupted() {

        int interruptedJobsCount = 0;

        for (ScanJob scanJob : scanJobRepository.findByStatusIn(ImmutableList.of(ScanJob.Status.STARTING, ScanJob.Status.STARTED))) {
            scanJobRepository.save(scanJob
                    .setStatus(ScanJob.Status.INTERRUPTED));
            interruptedJobsCount++;
        }
        
        if (interruptedJobsCount > 0) {
            logService.warn(logger, "Interrupted {} scan job(s).", interruptedJobsCount);
        }
    }
}
