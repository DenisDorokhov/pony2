package net.dorokhov.pony.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.library.domain.ScanJob;
import net.dorokhov.pony.api.log.service.LogService;
import net.dorokhov.pony.core.library.repository.ScanJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.annotation.Transactional;

public class ScanJobInterruptionRunner implements ApplicationRunner {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final ScanJobRepository scanJobRepository;
    private final LogService logService;

    public ScanJobInterruptionRunner(ScanJobRepository scanJobRepository, LogService logService) {
        this.scanJobRepository = scanJobRepository;
        this.logService = logService;
    }

    @Transactional
    public void markCurrentJobsAsInterrupted() {

        int interruptedJobsCount = 0;

        for (ScanJob scanJob : scanJobRepository.findByStatusIn(ImmutableList.of(ScanJob.Status.STARTING, ScanJob.Status.STARTED))) {
            scanJobRepository.save(ScanJob.builder(scanJob)
                    .status(ScanJob.Status.INTERRUPTED)
                    .build());
            interruptedJobsCount++;
        }
        
        if (interruptedJobsCount > 0) {
            logService.warn(logger, "Interrupted {} scan job(s).", interruptedJobsCount);
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        markCurrentJobsAsInterrupted();
    }
}
