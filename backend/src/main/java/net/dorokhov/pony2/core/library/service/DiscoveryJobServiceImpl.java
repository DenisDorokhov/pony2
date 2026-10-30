package net.dorokhov.pony2.core.library.service;

import com.google.common.base.Throwables;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.core.llm.repository.DiscoveryCacheRepository;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.discovery.AlbumDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.ArtistDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.FullDiscoveryService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryJob.Status.*;
import static net.dorokhov.pony2.core.library.LibraryConfig.DISCOVERY_JOB_EXECUTOR;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
public class DiscoveryJobServiceImpl implements DiscoveryJobService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryJobRepository discoveryJobRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository;
    private final DiscoveryCacheRepository discoveryCacheRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final FullDiscoveryService fullDiscoveryService;
    private final ArtistDiscoveryService artistDiscoveryService;
    private final AlbumDiscoveryService albumDiscoveryService;
    private final LogService logService;
    private final Executor discoveryJobExecutor;
    private final LibraryJobSynchronizer jobSynchronizer;

    private final TransactionTemplate transactionTemplate;

    private final Set<Observer> observers = synchronizedSet(new LinkedHashSet<>());

    private final AtomicReference<DiscoveryJobProgress> discoveryJobProgressReference = new AtomicReference<>();

    public DiscoveryJobServiceImpl(
            DiscoveryJobRepository discoveryJobRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository,
            DiscoveryCacheRepository discoveryCacheRepository,
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            FullDiscoveryService fullDiscoveryService,
            LibraryJobSynchronizer jobSynchronizer,
            ArtistDiscoveryService artistDiscoveryService,
            AlbumDiscoveryService albumDiscoveryService,
            LogService logService,
            @Qualifier(DISCOVERY_JOB_EXECUTOR) Executor discoveryJobExecutor,
            PlatformTransactionManager transactionManager
    ) {

        this.discoveryJobRepository = discoveryJobRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.artistDiscoveryIndexRepository = artistDiscoveryIndexRepository;
        this.discoveryCacheRepository = discoveryCacheRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.fullDiscoveryService = fullDiscoveryService;
        this.artistDiscoveryService = artistDiscoveryService;
        this.albumDiscoveryService = albumDiscoveryService;
        this.logService = logService;
        this.discoveryJobExecutor = discoveryJobExecutor;
        this.jobSynchronizer = jobSynchronizer;

        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    @Override
    public void cleanDiscoveryData() throws ConcurrentLibraryJobException {
        try (LibraryJobSynchronizer.LibraryJobRegistration ignored = jobSynchronizer.registerMaintenanceJob()) {
            while (true) {
                List<String> jobIds = requireNonNull(transactionTemplate.execute(status ->
                        discoveryJobRepository.findIdsForCleanup(PageRequest.of(0, 100))));
                if (jobIds.isEmpty()) {
                    break;
                }
                for (String jobId : jobIds) {
                    transactionTemplate.executeWithoutResult(status -> {
                        discoveryTaskRepository.deleteByJobId(jobId);
                        discoveryJobRepository.deleteById(jobId);
                    });
                }
            }
            transactionTemplate.executeWithoutResult(status -> artistDiscoveryIndexRepository.deleteAllInBatch());
            transactionTemplate.executeWithoutResult(status -> discoveryCacheRepository.deleteAllInBatch());
        }
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
    public @Nullable Optional<DiscoveryJobProgress> getCurrentDiscoveryJobProgress() {
        return Optional.ofNullable(discoveryJobProgressReference.get());
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable Optional<DiscoveryJobProgress> getDiscoveryJobProgress(String id) {
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
    @Transactional(readOnly = true)
    public long getTaskCount(String discoveryJobId) {
        return discoveryTaskRepository.countByJobId(discoveryJobId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscoveryTask> getAllTasks(DiscoveryTaskFilter filter, Pageable pageable) {
        DiscoveryTaskFilter.Result result = filter.result();
        DiscoveryTaskType type = filter.type();
        if (result == DiscoveryTaskFilter.Result.FAILED) {
            return type != null ? discoveryTaskRepository.findFailedByType(type, pageable)
                    : discoveryTaskRepository.findFailed(pageable);
        }
        if (result == DiscoveryTaskFilter.Result.NO_RESULT) {
            return type != null ? discoveryTaskRepository.findWithoutResultByType(type, pageable)
                    : discoveryTaskRepository.findWithoutResult(pageable);
        }
        return type != null ? discoveryTaskRepository.findByType(type, pageable)
                : discoveryTaskRepository.findAllTasks(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscoveryTask> getJobTasks(String discoveryJobId, DiscoveryTaskFilter filter, Pageable pageable) {
        DiscoveryTaskFilter.Result result = filter.result();
        DiscoveryTaskType type = filter.type();
        if (result == DiscoveryTaskFilter.Result.FAILED) {
            return type != null ? discoveryTaskRepository.findFailedByJobIdAndType(discoveryJobId, type, pageable)
                    : discoveryTaskRepository.findFailedByJobId(discoveryJobId, pageable);
        }
        if (result == DiscoveryTaskFilter.Result.NO_RESULT) {
            return type != null ? discoveryTaskRepository.findWithoutResultByJobIdAndType(discoveryJobId, type, pageable)
                    : discoveryTaskRepository.findWithoutResultByJobId(discoveryJobId, pageable);
        }
        return type != null ? discoveryTaskRepository.findByJobIdAndType(discoveryJobId, type, pageable)
                : discoveryTaskRepository.findByJobId(discoveryJobId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscoveryTask> getArtistTasks(String artistId, DiscoveryTaskFilter filter, Pageable pageable) {
        return discoveryTaskRepository.findByArtistId(artistId, filter.type(),
                filter.result() == DiscoveryTaskFilter.Result.FAILED,
                filter.result() == DiscoveryTaskFilter.Result.NO_RESULT, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscoveryTask> getAlbumTasks(String albumId, DiscoveryTaskFilter filter, Pageable pageable) {
        return discoveryTaskRepository.findByAlbumId(albumId, filter.type(),
                filter.result() == DiscoveryTaskFilter.Result.FAILED,
                filter.result() == DiscoveryTaskFilter.Result.NO_RESULT, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiscoveryTask> getTaskById(String id) {
        return discoveryTaskRepository.findById(id);
    }

    @Override
    @Transactional
    public DiscoveryJob startFullJob() throws ConcurrentLibraryJobException {
        return startFullJob(true);
    }

    @Override
    @Transactional
    public DiscoveryJob startFullJob(boolean cacheEnabled) throws ConcurrentLibraryJobException {
        return startDiscoveryJob(DiscoveryType.FULL, null, cacheEnabled);
    }

    @Override
    @Transactional
    public DiscoveryJob startArtistJob(String artistId) throws ConcurrentLibraryJobException {
        return startArtistJob(artistId, true);
    }

    @Override
    @Transactional
    public DiscoveryJob startArtistJob(String artistId, boolean cacheEnabled) throws ConcurrentLibraryJobException {
        return startDiscoveryJob(DiscoveryType.ARTIST, artistId, cacheEnabled);
    }

    @Override
    @Transactional
    public DiscoveryJob startAlbumJob(String albumId) throws ConcurrentLibraryJobException {
        return startAlbumJob(albumId, true);
    }

    @Override
    @Transactional
    public DiscoveryJob startAlbumJob(String albumId, boolean cacheEnabled) throws ConcurrentLibraryJobException {
        return startDiscoveryJob(DiscoveryType.ALBUM, albumId, cacheEnabled);
    }

    private DiscoveryJob startDiscoveryJob(DiscoveryType discoveryType, @Nullable String targetId, boolean cacheEnabled) throws ConcurrentLibraryJobException {
        LibraryJobSynchronizer.LibraryJobRegistration jobRegistration = jobSynchronizer.registerDiscoveryJob();
        try {
            return doStartDiscoveryJob(discoveryType, targetId, cacheEnabled, jobRegistration);
        } catch (Throwable t) {
            jobRegistration.close();
            throw t;
        }
    }

    private DiscoveryJob doStartDiscoveryJob(DiscoveryType discoveryType, @Nullable String targetId, boolean cacheEnabled, LibraryJobSynchronizer.LibraryJobRegistration jobRegistration) {

        DiscoveryJob startingJob = new DiscoveryJob().setType(discoveryType).setStatus(STARTING);
        switch (discoveryType) {
            case FULL -> {}
            case ARTIST -> startingJob.setArtist(artistRepository.findById(
                    requireNonNull(targetId, "Artist ID must not be null.")).orElseThrow());
            case ALBUM -> startingJob.setAlbum(albumRepository.findById(
                    requireNonNull(targetId, "Album ID must not be null.")).orElseThrow());
            case GENRE, SONG ->
                    throw new UnsupportedOperationException("Discovery type " + discoveryType + " is not supported yet.");
        }
        String targetName = switch (startingJob.getType()) {
            case FULL -> null;
            case GENRE -> startingJob.getGenre() != null ? startingJob.getGenre().getName() : null;
            case ARTIST -> startingJob.getArtist() != null ? startingJob.getArtist().getName() : null;
            case ALBUM -> startingJob.getAlbum() != null ? startingJob.getAlbum().getName() : null;
            case SONG -> startingJob.getSong() != null ? startingJob.getSong().getName() : null;
        };
        String targetDescription = startingJob.getType().name().toLowerCase(Locale.ROOT) +
                (targetName != null ? " '" + targetName + "'" : "");
        String cacheNote = cacheEnabled ? "" : " (cache disabled)";
        Optional<LogMessage> logStarting = logService.info(logger, "{}{}.",
                discoveryType == DiscoveryType.FULL ? "Starting full discovery job" : "Starting discovery job for " + targetDescription,
                cacheNote);
        DiscoveryJob discoveryJob = discoveryJobRepository.save(startingJob.setLogMessage(logStarting.orElse(null)));

        registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                onDiscoveryJobStatusChange(discoveryJob);
                discoveryJobExecutor.execute(() ->
                        runDiscoveryJob(discoveryJob, discoveryType, cacheEnabled, targetDescription, jobRegistration));
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    jobRegistration.close();
                }
            }
        });

        return discoveryJob;
    }

    private void runDiscoveryJob(
            DiscoveryJob discoveryJob, DiscoveryType discoveryType, boolean cacheEnabled, String targetDescription,
            LibraryJobSynchronizer.LibraryJobRegistration jobRegistration
    ) {
        DiscoveryJob currentDiscoveryJob = discoveryJob;
        try {
            jobSynchronizer.interruptDiscoveryIfCancelled();
            currentDiscoveryJob = changeDiscoveryJobStatusInTransaction(() -> {
                String cacheNote = cacheEnabled ? "" : " (cache disabled)";
                Optional<LogMessage> logStarted = logService.info(logger, "{}{}.",
                        discoveryType == DiscoveryType.FULL ? "Full discovery job started" : "Discovery job started for " + targetDescription,
                        cacheNote);
                return discoveryJobRepository.save(discoveryJob
                        .setStatus(STARTED)
                        .setLogMessage(logStarted.orElse(null)));
            });
            doDiscoveryJob(currentDiscoveryJob, cacheEnabled);
            completeDiscoveryJob(currentDiscoveryJob);
        } catch (Throwable t) {
            failDiscoveryJob(discoveryJob, currentDiscoveryJob, discoveryType, targetDescription, t);
        } finally {
            discoveryJobProgressReference.set(null);
            jobRegistration.close();
        }
    }

    private void failDiscoveryJob(
            DiscoveryJob discoveryJob, DiscoveryJob currentDiscoveryJob, DiscoveryType discoveryType,
            String targetDescription, Throwable error
    ) {
        try {
            boolean interrupted = Throwables.getCausalChain(error).stream()
                    .anyMatch(cause -> cause instanceof DiscoveryInterruptedException);
            if (interrupted) {
                notifyObservers(observer -> observer.onDiscoveryJobInterrupting(currentDiscoveryJob));
            } else {
                notifyObservers(observer -> observer.onDiscoveryJobFailing(currentDiscoveryJob));
            }
            changeDiscoveryJobStatusInTransaction(() -> {
                if (interrupted) {
                    Optional<LogMessage> logMessage = logService.warn(logger, "Discovery job has been interrupted.", error);
                    return discoveryJobRepository.save(
                            discoveryJobRepository.findById(discoveryJob.getId()).orElseThrow()
                                    .setStatus(INTERRUPTED)
                                    .setLogMessage(logMessage.orElse(null)));
                } else {
                    Optional<LogMessage> logMessage = logService.error(logger,
                            "{} due to an unexpected error.",
                            discoveryType == DiscoveryType.FULL
                                    ? "Full discovery job failed"
                                    : "Discovery job failed for " + targetDescription,
                            error);
                    return discoveryJobRepository.save(
                            discoveryJobRepository.findById(discoveryJob.getId()).orElseThrow()
                                    .setStatus(FAILED)
                                    .setLogMessage(logMessage.orElse(null)));
                }
            });
        } catch (Throwable failure) {
            logger.error("Could not finalize failed discovery job '{}'.", discoveryJob.getId(), failure);
        }
    }

    private void doDiscoveryJob(DiscoveryJob discoveryJob, boolean cacheEnabled) {
        Consumer<DiscoveryProgress> progressObserver = discoveryProgress ->
                onDiscoveryJobProgress(new DiscoveryJobProgress(discoveryJob, discoveryProgress));
        switch (discoveryJob.getType()) {
            case FULL -> fullDiscoveryService.discover(discoveryJob, cacheEnabled, progressObserver);
            case ARTIST -> artistDiscoveryService.discover(
                    discoveryJob,
                    artistRepository.findById(requireNonNull(discoveryJob.getArtist(),
                            () -> "Discovery job '" + discoveryJob.getId() + "' has no associated artist.").getId()).orElseThrow(),
                    cacheEnabled,
                    progressObserver
            );
            case ALBUM -> albumDiscoveryService.discover(
                    discoveryJob,
                    albumRepository.findById(requireNonNull(discoveryJob.getAlbum(),
                            () -> "Discovery job '" + discoveryJob.getId() + "' has no associated album.").getId()).orElseThrow(),
                    cacheEnabled,
                    progressObserver
            );
            case GENRE, SONG -> throw new UnsupportedOperationException("Discovery type " + discoveryJob.getType() + " is not supported yet.");
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
            DiscoveryJob completedJob = discoveryJobRepository.save(discoveryJob.setStatus(status));

            String targetName = switch (completedJob.getType()) {
                case FULL -> null;
                case GENRE -> completedJob.getGenre() != null ? completedJob.getGenre().getName() : null;
                case ARTIST -> completedJob.getArtist() != null ? completedJob.getArtist().getName() : null;
                case ALBUM -> completedJob.getAlbum() != null ? completedJob.getAlbum().getName() : null;
                case SONG -> completedJob.getSong() != null ? completedJob.getSong().getName() : null;
            };
            String jobDescription = completedJob.getType() == DiscoveryType.FULL
                    ? "Full discovery job"
                    : "Discovery job";
            String targetDescription = completedJob.getType() == DiscoveryType.FULL
                    ? ""
                    : " for " + completedJob.getType().name().toLowerCase(Locale.ROOT) +
                            (targetName != null ? " '" + targetName + "'" : "");

            Optional<LogMessage> logMessage = switch (status) {
                case COMPLETE -> logService.info(logger,
                        "{} completed{}, completed tasks: {}, failed tasks: {}.",
                        jobDescription, targetDescription, completedTasks, failedTasks);
                case MODERATE -> logService.warn(logger,
                        "{} completed with errors{}, completed tasks: {}, failed tasks: {}.",
                        jobDescription, targetDescription, completedTasks, failedTasks);
                case FAILED -> logService.error(logger,
                        "{} failed{}, completed tasks: {}, failed tasks: {}.",
                        jobDescription, targetDescription, completedTasks, failedTasks);
                default -> throw new IllegalStateException("Unexpected discovery job status.");
            };
            return completedJob.setLogMessage(logMessage.orElse(null));
        });
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
            } catch (Throwable t) {
                logger.error("Could not call discovery job observer {}.", observer, t);
            }
        }
    }

    private DiscoveryJob changeDiscoveryJobStatusInTransaction(Supplier<DiscoveryJob> handler) {
        return onDiscoveryJobStatusChange(transactionTemplate.execute(transactionStatus -> handler.get()));
    }
}
