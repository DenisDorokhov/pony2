package net.dorokhov.pony.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.repository.ScanJobRepository;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public void markCurrentJobsAsInterrupted() throws ConcurrentScanException {

        int interruptedJobsCount = 0;

        Pageable pageable = new PageRequest(0, 100);
        while (pageable != null) {
            Page<ScanJob> scanJobs = scanJobRepository.findByStatusIn(ImmutableList.of(ScanJob.Status.STARTING, ScanJob.Status.STARTED), pageable);
            for (ScanJob scanJob : scanJobs) {
                scanJobRepository.save(ScanJob.builder(scanJob)
                        .status(ScanJob.Status.INTERRUPTED)
                        .build());
                interruptedJobsCount++;
            }
            pageable = scanJobs.nextPageable();
        }
        
        if (interruptedJobsCount > 0) {
            logService.warn(logger, "Interrupted {} scan job(s).", interruptedJobsCount);
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        markCurrentJobsAsInterrupted();
    }
}
