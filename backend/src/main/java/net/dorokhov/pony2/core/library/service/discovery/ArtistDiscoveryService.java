package net.dorokhov.pony2.core.library.service.discovery;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.discovery.task.SpotifyArtistDataService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ARTIST_DISCOVERY;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class ArtistDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtistDiscoveryRepository artistDiscoveryRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final SpotifyArtistDataService spotifyArtistDataService;
    private final LogService logService;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final TransactionTemplate transactionTemplate;

    public ArtistDiscoveryService(
            ArtistDiscoveryRepository artistDiscoveryRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            SpotifyArtistDataService spotifyArtistDataService,
            LogService logService,
            LibraryJobSynchronizer jobSynchronizer,
            PlatformTransactionManager transactionManager
    ) {
        this.artistDiscoveryRepository = artistDiscoveryRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.spotifyArtistDataService = spotifyArtistDataService;
        this.logService = logService;
        this.jobSynchronizer = jobSynchronizer;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public void discover(DiscoveryJob discoveryJob, Artist artist, boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer) {
        notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, null), observer);
        ArtistDiscovery discovery = createDiscovery(discoveryJob, artist);
        discoverSpotifyArtistData(discovery, cacheEnabled);
    }

    private ArtistDiscovery createDiscovery(DiscoveryJob discoveryJob, Artist artist) {
        return requireNonNull(transactionTemplate.execute(status ->
                artistDiscoveryRepository.save(new ArtistDiscovery()
                        .setArtist(artist)
                        .setJob(discoveryJob))));
    }

    private TaskResult<SpotifyArtistData> discoverSpotifyArtistData(ArtistDiscovery discovery, boolean cacheEnabled) {
        Artist artist = discovery.getArtist();
        return executeTask(
                discovery,
                DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
                new DiscoveryTask.ArtistParameter(artist.getId()),
                task -> spotifyArtistDataService.discover(task, cacheEnabled).orElse(null),
                error -> logService.error(logger, "Could not discover Spotify data for artist '{} -> {}'.",
                        artist.getId(), artist.getName(), error)
        );
    }

    private <P, R> TaskResult<R> executeTask(
            ArtistDiscovery discovery,
            DiscoveryTaskType type,
            P parameter,
            Function<DiscoveryTask, R> action,
            Consumer<RuntimeException> errorHandler
    ) {
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignored = jobSynchronizer.registerDiscoveryTask()) {
            return executeRegisteredTask(discovery, type, parameter, action, errorHandler);
        }
    }

    private <P, R> TaskResult<R> executeRegisteredTask(
            ArtistDiscovery discovery, DiscoveryTaskType type, P parameter,
            Function<DiscoveryTask, R> action, Consumer<RuntimeException> errorHandler
    ) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        DiscoveryTask task = startTask(discovery, type, parameter);
        Artist artist = discovery.getArtist();
        logger.debug("Started discovery task '{}' of type {} for artist '{} -> {}' in job '{}'.",
                task.getId(), type, artist.getId(), artist.getName(), discovery.getJob().getId());
        try {
            R result = action.apply(task);
            saveResult(task, DiscoveryTask.Status.COMPLETE, JsonConverter.toJson(result));
            logger.debug("Completed discovery task '{}' of type {} for artist '{} -> {}' in job '{}' after {} ms.",
                    task.getId(), type, artist.getId(), artist.getName(), discovery.getJob().getId(),
                    stopwatch.elapsed().toMillis());
            return new TaskResult<>(DiscoveryTask.Status.COMPLETE, result);
        } catch (DiscoveryInterruptedException e) {
            saveResult(task, DiscoveryTask.Status.INTERRUPTED, null);
            logger.info("Interrupted execution of discovery task '{}' of type {} for artist '{} -> {}' in job '{}' after {} ms.",
                    task.getId(), type, artist.getId(), artist.getName(), discovery.getJob().getId(),
                    stopwatch.elapsed().toMillis());
            throw e;
        } catch (RuntimeException e) {
            saveResult(task, DiscoveryTask.Status.FAILED, JsonConverter.toJson(new DiscoveryTask.ErrorResult(Throwables.getStackTraceAsString(e))));
            logger.warn("Failed discovery task '{}' of type {} for artist '{} -> {}' in job '{}' after {} ms.",
                    task.getId(), type, artist.getId(), artist.getName(), discovery.getJob().getId(),
                    stopwatch.elapsed().toMillis());
            errorHandler.accept(e);
            return new TaskResult<>(DiscoveryTask.Status.FAILED, null);
        }
    }

    private <P> DiscoveryTask startTask(ArtistDiscovery discovery, DiscoveryTaskType type, P parameter) {
        DiscoveryTask task = requireNonNull(transactionTemplate.execute(status -> {
            ArtistDiscovery persistedDiscovery = artistDiscoveryRepository.findById(discovery.getId()).orElseThrow();
            DiscoveryTask startedTask = discoveryTaskRepository.save(new DiscoveryTask()
                    .setJob(persistedDiscovery.getJob())
                    .setType(type)
                    .setStatus(DiscoveryTask.Status.STARTED)
                    .setParameter(JsonConverter.toJson(parameter)));
            persistedDiscovery.getTasks().add(startedTask);
            return startedTask;
        }));
        discovery.getTasks().add(task);
        return task;
    }

    private void saveResult(DiscoveryTask task, DiscoveryTask.Status status, @Nullable String result) {
        transactionTemplate.executeWithoutResult(transactionStatus ->
                discoveryTaskRepository.save(task
                        .setStatus(status)
                        .setResult(result)));
    }

    private void notifyProgressObserver(DiscoveryProgress discoveryProgress, @Nullable Consumer<DiscoveryProgress> handler) {
        if (handler != null) {
            try {
                handler.accept(discoveryProgress);
            } catch (Exception e) {
                logger.error("Could not call discovery progress observer.", e);
            }
        }
    }

    private record TaskResult<R>(DiscoveryTask.Status status, @Nullable R value) {}
}
