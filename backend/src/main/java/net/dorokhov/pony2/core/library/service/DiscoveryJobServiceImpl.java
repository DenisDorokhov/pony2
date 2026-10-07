package net.dorokhov.pony2.core.library.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentDiscoveryException;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.discovery.AlbumDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.ArtistDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.FullDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.synchronizedSet;
import static net.dorokhov.pony2.api.library.domain.DiscoveryJob.Status.*;
import static net.dorokhov.pony2.core.library.LibraryConfig.DISCOVERY_JOB_EXECUTOR;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
public class DiscoveryJobServiceImpl implements DiscoveryJobService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryJobRepository discoveryJobRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final FullDiscoveryService fullDiscoveryService;
    private final ArtistDiscoveryService artistDiscoveryService;
    private final AlbumDiscoveryService albumDiscoveryService;
    private final LogService logService;
    private final Executor discoveryJobExecutor;

    private final TransactionTemplate transactionTemplate;

    private final Set<Observer> observers = synchronizedSet(new LinkedHashSet<>());

    private final AtomicReference<DiscoveryJobProgress> discoveryJobProgressReference = new AtomicReference<>();

    // ReentrantLock doesn't fit here, because we release in different thread.
    private final Semaphore discoveryJobSemaphore = new Semaphore(1);

    public DiscoveryJobServiceImpl(
            DiscoveryJobRepository discoveryJobRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            FullDiscoveryService fullDiscoveryService,
            ArtistDiscoveryService artistDiscoveryService,
            AlbumDiscoveryService albumDiscoveryService,
            LogService logService,
            @Qualifier(DISCOVERY_JOB_EXECUTOR) Executor discoveryJobExecutor,
            PlatformTransactionManager transactionManager
    ) {

        this.discoveryJobRepository = discoveryJobRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.fullDiscoveryService = fullDiscoveryService;
        this.artistDiscoveryService = artistDiscoveryService;
        this.albumDiscoveryService = albumDiscoveryService;
        this.logService = logService;
        this.discoveryJobExecutor = discoveryJobExecutor;

        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(checkNotNull(observer));
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(checkNotNull(observer));
    }

    @Override
    @Nullable
    public Optional<DiscoveryJobProgress> getCurrentDiscoveryJobProgress() {
        return Optional.ofNullable(discoveryJobProgressReference.get());
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public Optional<DiscoveryJobProgress> getDiscoveryJobProgress(String id) {
        DiscoveryJobProgress discoveryJobProgress = discoveryJobProgressReference.get();
        if (discoveryJobProgress != null && id.equals(discoveryJobProgress.getDiscoveryJob().getId())) {
            return Optional.of(discoveryJobProgress);
        } else {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscoveryJob> getAll(Pageable pageable) {
        return discoveryJobRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiscoveryJob> getById(String id) {
        return discoveryJobRepository.findById(id);
    }

    @Override
    @Transactional
    public DiscoveryJob startFullJob() throws ConcurrentDiscoveryException {
        return doStartDiscoveryJob(DiscoveryType.FULL, null);
    }

    @Override
    @Transactional
    public DiscoveryJob startArtistJob(String artistId) throws ConcurrentDiscoveryException {
        return doStartDiscoveryJob(DiscoveryType.ARTIST, artistId);
    }

    @Override
    @Transactional
    public DiscoveryJob startAlbumJob(String albumId) throws ConcurrentDiscoveryException {
        return doStartDiscoveryJob(DiscoveryType.ALBUM, albumId);
    }

    private DiscoveryJob doStartDiscoveryJob(DiscoveryType discoveryType, @Nullable String parameter) throws ConcurrentDiscoveryException {

        if (!discoveryJobSemaphore.tryAcquire()) {
            throw new ConcurrentDiscoveryException();
        }

        String jobDescription = discoveryJobDescription(discoveryType, parameter);
        Optional<LogMessage> logStarting = logService.info(logger, "Starting discovery job {}...", jobDescription);
        DiscoveryJob discoveryJob = discoveryJobRepository.save(new DiscoveryJob()
                .setType(discoveryType)
                .setStatus(STARTING)
                .setParameter(parameter)
                .setLogMessage(logStarting.orElse(null)));

        registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                onDiscoveryJobStatusChange(discoveryJob);
                discoveryJobExecutor.execute(() -> {
                    DiscoveryJob currentDiscoveryJob = discoveryJob;
                    try {
                        currentDiscoveryJob = changeDiscoveryJobStatusInTransaction(() -> {
                            Optional<LogMessage> logStarted = logService.info(logger, "Started discovery job {}.", jobDescription);
                            return discoveryJobRepository.save(discoveryJob
                                    .setStatus(STARTED)
                                    .setLogMessage(logStarted.orElse(null)));
                        });
                        doDiscoveryJob(currentDiscoveryJob);
                        completeDiscoveryJob(currentDiscoveryJob);
                    } catch (Exception e) {
                        DiscoveryJob failedDiscoveryJob = currentDiscoveryJob;
                        notifyObservers(observer -> observer.onDiscoveryJobFailing(failedDiscoveryJob));
                        changeDiscoveryJobStatusInTransaction(() -> {
                            Optional<LogMessage> logFailed = logService.error(logger,
                                    "Unexpected error occurred when performing discovery job " + jobDescription + ".", e);
                            return discoveryJobRepository.save(
                                    discoveryJobRepository.findById(discoveryJob.getId()).orElseThrow()
                                            .setStatus(FAILED)
                                            .setLogMessage(logFailed.orElse(null)));
                        });
                    } finally {
                        discoveryJobProgressReference.set(null);
                        discoveryJobSemaphore.release();
                    }
                });
            }
        });

        return discoveryJob;
    }

    private void doDiscoveryJob(DiscoveryJob discoveryJob) {
        Consumer<DiscoveryProgress> progressObserver = discoveryProgress ->
                onDiscoveryJobProgress(new DiscoveryJobProgress(discoveryJob, discoveryProgress));
        switch (discoveryJob.getType()) {
            case FULL -> fullDiscoveryService.discover(discoveryJob, progressObserver);
            case ARTIST -> artistDiscoveryService.discover(
                    discoveryJob,
                    artistRepository.findById(Objects.requireNonNull(discoveryJob.getParameter())).orElseThrow(),
                    progressObserver
            );
            case ALBUM -> albumDiscoveryService.discover(
                    discoveryJob,
                    albumRepository.findById(Objects.requireNonNull(discoveryJob.getParameter())).orElseThrow(),
                    progressObserver
            );
        }
    }

    private void completeDiscoveryJob(DiscoveryJob discoveryJob) {

        long totalTasks = discoveryTaskRepository.countByJobId(discoveryJob.getId());
        long completedTasks = discoveryTaskRepository.countByJobIdAndStatus(discoveryJob.getId(), DiscoveryTask.Status.COMPLETE);
        long failedTasks = totalTasks - completedTasks;

        DiscoveryJob.Status status;
        if (totalTasks == 0 || failedTasks == 0) {
            status = COMPLETE;
        } else if (completedTasks == 0) {
            status = FAILED;
        } else {
            status = MODERATE;
        }

        switch (status) {
            case COMPLETE -> notifyObservers(observer -> observer.onDiscoveryJobCompleting(discoveryJob));
            case MODERATE -> notifyObservers(observer -> observer.onDiscoveryJobModerating(discoveryJob));
            case FAILED -> notifyObservers(observer -> observer.onDiscoveryJobFailing(discoveryJob));
            default -> throw new IllegalStateException("Unexpected discovery job status.");
        }

        changeDiscoveryJobStatusInTransaction(() -> {
            DiscoveryResult discoveryResult = new DiscoveryResult()
                    .setType(discoveryJob.getType())
                    .setCompletedTasks(completedTasks)
                    .setFailedTasks(failedTasks);
            return discoveryJobRepository.save(discoveryJob
                    .setStatus(status)
                    .setDiscoveryResult(discoveryResult)
                    .setLogMessage(completionLogMessage(discoveryJob, status, completedTasks, failedTasks).orElse(null)));
        });
    }

    private Optional<LogMessage> completionLogMessage(
            DiscoveryJob discoveryJob,
            DiscoveryJob.Status status,
            long completedTasks,
            long failedTasks
    ) {
        String resultDescription = String.format(
                "%s. Completed tasks: %s, failed tasks: %s",
                discoveryJobDescription(discoveryJob.getType(), discoveryJob.getParameter()),
                completedTasks,
                failedTasks
        );
        return switch (status) {
            case COMPLETE -> logService.info(logger, "Discovery job complete: {}.", resultDescription);
            case MODERATE -> logService.warn(logger, "Discovery job complete with failed tasks: {}.", resultDescription);
            case FAILED -> logService.error(logger, "Discovery job failed: {}.", resultDescription);
            default -> throw new IllegalStateException("Unexpected discovery job status.");
        };
    }

    private String discoveryJobDescription(DiscoveryType discoveryType, @Nullable String parameter) {
        return parameter != null ? discoveryType + " for " + parameter : discoveryType.toString();
    }

    private DiscoveryJob onDiscoveryJobStatusChange(DiscoveryJob discoveryJob) {
        discoveryJobProgressReference.set(new DiscoveryJobProgress(discoveryJob, null));
        switch (discoveryJob.getStatus()) {
            case STARTING:
                notifyObservers(observer -> observer.onDiscoveryJobStarting(discoveryJob));
                break;
            case STARTED:
                notifyObservers(observer -> observer.onDiscoveryJobStarted(discoveryJob));
                break;
            case COMPLETE:
                notifyObservers(observer -> observer.onDiscoveryJobCompleted(discoveryJob));
                break;
            case MODERATE:
                notifyObservers(observer -> observer.onDiscoveryJobModerated(discoveryJob));
                break;
            case FAILED:
                notifyObservers(observer -> observer.onDiscoveryJobFailed(discoveryJob));
                break;
            case INTERRUPTED:
                notifyObservers(observer -> observer.onDiscoveryJobInterrupted(discoveryJob));
                break;
            default:
                throw new IllegalStateException("Unexpected discovery job status.");
        }
        return discoveryJob;
    }

    private void onDiscoveryJobProgress(DiscoveryJobProgress discoveryJobProgress) {
        discoveryJobProgressReference.set(discoveryJobProgress);
        notifyObservers(observer -> observer.onDiscoveryJobProgress(discoveryJobProgress));
    }

    private void notifyObservers(Consumer<Observer> handler) {
        for (Observer observer : new ArrayList<>(observers)) {
            try {
                handler.accept(observer);
            } catch (Exception e) {
                logger.error("Could not call discovery job observer {}.", observer, e);
            }
        }
    }

    private DiscoveryJob changeDiscoveryJobStatusInTransaction(Supplier<DiscoveryJob> handler) {
        return onDiscoveryJobStatusChange(transactionTemplate.execute(transactionStatus -> handler.get()));
    }
}
