package net.dorokhov.pony2.core.library.service.discovery;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class DiscoveryTaskExecutor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final TransactionTemplate transactionTemplate;

    public DiscoveryTaskExecutor(
            DiscoveryTaskRepository discoveryTaskRepository,
            LibraryJobSynchronizer jobSynchronizer,
            PlatformTransactionManager transactionManager
    ) {
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.jobSynchronizer = jobSynchronizer;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public <R> TaskResult<R> execute(DiscoveryTaskExecution<R> execution) {
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignored = jobSynchronizer.registerDiscoveryTask()) {
            return executeRegisteredTask(execution);
        }
    }

    private <R> TaskResult<R> executeRegisteredTask(DiscoveryTaskExecution<R> execution) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        DiscoveryTask task = execution.startTask();
        logger.debug("Started discovery task '{}' of type {} in job '{}'.",
                task.getId(), task.getType(), task.getJob().getId());
        R result;
        try {
            result = execution.executeTask(task);
            saveResult(task, DiscoveryTask.Status.COMPLETE, JsonConverter.toJson(result));
        } catch (DiscoveryInterruptedException e) {
            saveResult(task, DiscoveryTask.Status.INTERRUPTED, null);
            logger.info("Interrupted execution of discovery task '{}' of type {} in job '{}' after {}.",
                    task.getId(), task.getType(), task.getJob().getId(), stopwatch.elapsed());
            throw e;
        } catch (RuntimeException e) {
            saveResult(task, DiscoveryTask.Status.FAILED, JsonConverter.toJson(new DiscoveryTask.ErrorResult(Throwables.getStackTraceAsString(e))));
            logger.warn("Failed discovery task '{}' of type {} in job '{}' after {}.",
                    task.getId(), task.getType(), task.getJob().getId(), stopwatch.elapsed());
            execution.onError(task, e);
            return new TaskResult<>(DiscoveryTask.Status.FAILED, null);
        }
        logger.debug("Completed discovery task '{}' of type {} in job '{}' after {}.",
                task.getId(), task.getType(), task.getJob().getId(), stopwatch.elapsed());
        execution.onCompletion(task, result);
        return new TaskResult<>(DiscoveryTask.Status.COMPLETE, result);
    }

    private void saveResult(DiscoveryTask task, DiscoveryTask.Status status, @Nullable String result) {
        transactionTemplate.executeWithoutResult(transactionStatus ->
                discoveryTaskRepository.save(task
                        .setStatus(status)
                        .setResult(result)));
    }

    public record TaskResult<R>(DiscoveryTask.Status status, @Nullable R value) {}
}
