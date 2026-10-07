package net.dorokhov.pony2.core.library.service.discovery;

import com.google.common.collect.ImmutableList;
import jakarta.annotation.PostConstruct;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiscoveryJobInterruptionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryJobRepository discoveryJobRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final LogService logService;

    public DiscoveryJobInterruptionService(
            DiscoveryJobRepository discoveryJobRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            LogService logService
    ) {
        this.discoveryJobRepository = discoveryJobRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.logService = logService;
    }

    @PostConstruct
    public void markCurrentJobsAsInterrupted() {

        List<DiscoveryJob> interruptedJobs = discoveryJobRepository.findByStatusIn(ImmutableList.of(DiscoveryJob.Status.STARTING, DiscoveryJob.Status.STARTED));
        for (DiscoveryJob discoveryJob : interruptedJobs) {
            discoveryJobRepository.save(discoveryJob
                    .setStatus(DiscoveryJob.Status.INTERRUPTED));
        }

        List<DiscoveryTask> interruptedTasks = discoveryTaskRepository.findByStatus(DiscoveryTask.Status.STARTED);
        for (DiscoveryTask task : interruptedTasks) {
            discoveryTaskRepository.save(task
                    .setStatus(DiscoveryTask.Status.INTERRUPTED));
        }

        if (!interruptedJobs.isEmpty()) {
            logService.warn(logger, "Interrupted {} discovery job(s).", interruptedJobs.size());
        }
        if (!interruptedTasks.isEmpty()) {
            logService.warn(logger, "Interrupted {} discovery task(s).", interruptedTasks.size());
        }
    }
}
