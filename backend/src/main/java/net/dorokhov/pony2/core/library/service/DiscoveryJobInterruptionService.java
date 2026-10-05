package net.dorokhov.pony2.core.library.service;

import com.google.common.collect.ImmutableList;
import jakarta.annotation.PostConstruct;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryJobInterruptionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryJobRepository discoveryJobRepository;
    private final LogService logService;

    public DiscoveryJobInterruptionService(
            DiscoveryJobRepository discoveryJobRepository,
            LogService logService
    ) {
        this.discoveryJobRepository = discoveryJobRepository;
        this.logService = logService;
    }

    @PostConstruct
    public void markCurrentJobsAsInterrupted() {

        int interruptedJobsCount = 0;

        for (DiscoveryJob discoveryJob : discoveryJobRepository.findByStatusIn(ImmutableList.of(DiscoveryJob.Status.STARTING, DiscoveryJob.Status.STARTED))) {
            discoveryJobRepository.save(discoveryJob
                    .setStatus(DiscoveryJob.Status.INTERRUPTED));
            interruptedJobsCount++;
        }

        if (interruptedJobsCount > 0) {
            logService.warn(logger, "Interrupted {} discovery job(s).", interruptedJobsCount);
        }
    }
}
