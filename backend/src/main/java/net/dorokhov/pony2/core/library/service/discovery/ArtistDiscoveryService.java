package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.discovery.task.SpotifyArtistDataService;
import net.dorokhov.pony2.core.library.service.discovery.task.SpotifyTopTracksService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ARTIST_DISCOVERY;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class ArtistDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtistDiscoveryRepository artistDiscoveryRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final SpotifyArtistDataService spotifyArtistDataService;
    private final SpotifyTopTracksService spotifyTopTracksService;
    private final LogService logService;
    private final DiscoveryTaskExecutor taskExecutor;
    private final ConfigService configService;
    private final TransactionTemplate transactionTemplate;

    public ArtistDiscoveryService(
            ArtistDiscoveryRepository artistDiscoveryRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            SpotifyArtistDataService spotifyArtistDataService,
            SpotifyTopTracksService spotifyTopTracksService,
            LogService logService,
            DiscoveryTaskExecutor taskExecutor,
            ConfigService configService,
            PlatformTransactionManager transactionManager
    ) {
        this.artistDiscoveryRepository = artistDiscoveryRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.spotifyArtistDataService = spotifyArtistDataService;
        this.spotifyTopTracksService = spotifyTopTracksService;
        this.logService = logService;
        this.taskExecutor = taskExecutor;
        this.configService = configService;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public void discover(DiscoveryJob discoveryJob, Artist artist, boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer) {
        notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, null), observer);
        ArtistDiscovery artistDiscovery = createDiscovery(discoveryJob, artist);
        boolean llmEnabled = configService.get().llmEnabled();
        if (llmEnabled) {
            taskExecutor.execute(new SpotifyArtistDataTaskExecution(artistDiscovery, cacheEnabled));
            String spotifyArtistDataTaskId = artistDiscovery.getTasks().getLast().getId();
            taskExecutor.execute(new SpotifyTopTracksTaskExecution(artistDiscovery, spotifyArtistDataTaskId, cacheEnabled));
        }
    }

    private ArtistDiscovery createDiscovery(DiscoveryJob discoveryJob, Artist artist) {
        return requireNonNull(transactionTemplate.execute(status ->
                artistDiscoveryRepository.save(new ArtistDiscovery()
                        .setArtist(artist)
                        .setJob(discoveryJob))));
    }

    private DiscoveryTask startTask(ArtistDiscovery discovery, DiscoveryTaskType type, Object parameter) {
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

    private void notifyProgressObserver(DiscoveryProgress discoveryProgress, @Nullable Consumer<DiscoveryProgress> handler) {
        if (handler != null) {
            try {
                handler.accept(discoveryProgress);
            } catch (Exception e) {
                logger.error("Could not call discovery progress observer.", e);
            }
        }
    }

    private class SpotifyArtistDataTaskExecution implements DiscoveryTaskExecution<SpotifyArtistData> {

        private final ArtistDiscovery discovery;
        private final boolean cacheEnabled;

        private SpotifyArtistDataTaskExecution(ArtistDiscovery discovery, boolean cacheEnabled) {
            this.discovery = discovery;
            this.cacheEnabled = cacheEnabled;
        }

        @Override
        public DiscoveryTask startTask() {
            return ArtistDiscoveryService.this.startTask(
                    discovery,
                    DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
                    new DiscoveryTask.ArtistParameter(discovery.getArtist().getId())
            );
        }

        @Nullable
        @Override
        public SpotifyArtistData executeTask(DiscoveryTask task) {
            return spotifyArtistDataService.discover(task, cacheEnabled).orElse(null);
        }

        @Override
        public void onError(DiscoveryTask task, RuntimeException error) {
            Artist artist = discovery.getArtist();
            logService.error(logger, "Could not discover Spotify data for artist '{} -> {}'.",
                    artist.getId(), artist.getName(), error);
        }
    }

    private class SpotifyTopTracksTaskExecution implements DiscoveryTaskExecution<List<String>> {

        private final ArtistDiscovery discovery;
        private final String spotifyArtistDataTaskId;
        private final boolean cacheEnabled;

        private SpotifyTopTracksTaskExecution(ArtistDiscovery discovery, String spotifyArtistDataTaskId, boolean cacheEnabled) {
            this.discovery = discovery;
            this.spotifyArtistDataTaskId = spotifyArtistDataTaskId;
            this.cacheEnabled = cacheEnabled;
        }

        @Override
        public DiscoveryTask startTask() {
            return ArtistDiscoveryService.this.startTask(
                    discovery,
                    DiscoveryTaskType.SPOTIFY_TOP_TRACKS,
                    new DiscoveryTask.SpotifyTopTracksParameter(discovery.getArtist().getId(), spotifyArtistDataTaskId)
            );
        }

        @Override
        public List<String> executeTask(DiscoveryTask task) {
            return spotifyTopTracksService.discover(task, cacheEnabled);
        }

        @Override
        public void onError(DiscoveryTask task, RuntimeException error) {
            Artist artist = discovery.getArtist();
            logService.error(logger, "Could not match Spotify top tracks for artist '{} -> {}'.",
                    artist.getId(), artist.getName(), error);
        }
    }
}
