package net.dorokhov.pony2.core.library.service.discovery;

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
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
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
    private final LogService logService;
    private final TransactionTemplate transactionTemplate;

    public ArtistDiscoveryService(
            ArtistDiscoveryRepository artistDiscoveryRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            SpotifyArtistDataService spotifyArtistDataService,
            LogService logService,
            PlatformTransactionManager transactionManager
    ) {
        this.artistDiscoveryRepository = artistDiscoveryRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.spotifyArtistDataService = spotifyArtistDataService;
        this.logService = logService;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public void discover(DiscoveryJob discoveryJob, Artist artist, @Nullable Consumer<DiscoveryProgress> observer) {
        notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, null), observer);
        DiscoveryTask task = requireNonNull(transactionTemplate.execute(status -> {
            DiscoveryTask startedTask = discoveryTaskRepository.save(new DiscoveryTask()
                    .setJob(discoveryJob)
                    .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                    .setStatus(DiscoveryTask.Status.STARTED)
                    .setArgument(JsonConverter.toJson(new DiscoveryTask.ArtistArgument(artist.getId()))));
            artistDiscoveryRepository.save(new ArtistDiscovery()
                    .setArtist(artist)
                    .setJob(discoveryJob)
                    .setTasks(List.of(startedTask)));
            return startedTask;
        }));
        try {
            Optional<SpotifyArtistData> result = spotifyArtistDataService.discover(artist.getId());
            saveResult(task, DiscoveryTask.Status.COMPLETE, JsonConverter.toJson(result.orElse(null)));
        } catch (DiscoveryInterruptedException e) {
            throw e;
        } catch (RuntimeException e) {
            saveResult(task, DiscoveryTask.Status.FAILED, JsonConverter.toJson(new DiscoveryTask.ErrorResult(Throwables.getStackTraceAsString(e))));
            logService.error(logger, "Could not discover Spotify data for artist '{}' ({}).",
                    artist.getName(), artist.getId(), e);
        }
    }

    private void saveResult(DiscoveryTask task, DiscoveryTask.Status status, String result) {
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
}
