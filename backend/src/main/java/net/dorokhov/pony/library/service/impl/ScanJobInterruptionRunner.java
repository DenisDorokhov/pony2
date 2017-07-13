package net.dorokhov.pony.library.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.common.PageWalker;
import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.repository.ScanJobRepository;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

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

        AtomicInteger interruptedJobsCount = new AtomicInteger();
        PageWalker.walk(new PageRequest(0, 100), (ScanJob scanJob) -> {
            scanJobRepository.save(ScanJob.builder(scanJob)
                    .status(ScanJob.Status.INTERRUPTED)
                    .build());
            interruptedJobsCount.incrementAndGet();
        }, pageable -> scanJobRepository.findByStatusIn(ImmutableList.of(ScanJob.Status.STARTING, ScanJob.Status.STARTED), pageable));

        if (interruptedJobsCount.get() > 0) {
            logService.warn(logger, "Interrupted {} scan job(s).", interruptedJobsCount.get());
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        markCurrentJobsAsInterrupted();
    }
}
