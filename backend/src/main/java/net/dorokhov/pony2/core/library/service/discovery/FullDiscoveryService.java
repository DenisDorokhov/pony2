package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.ArtistDiscoveryService.DiscoveryPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_PLANNING;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_PROCESSING;

@Service
public class FullDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final ArtistDiscoveryService artistDiscoveryService;
    private final LibraryJobSynchronizer jobSynchronizer;

    public FullDiscoveryService(
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            ArtistDiscoveryService artistDiscoveryService,
            LibraryJobSynchronizer jobSynchronizer
    ) {
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.artistDiscoveryService = artistDiscoveryService;
        this.jobSynchronizer = jobSynchronizer;
    }

    public void discover(DiscoveryJob discoveryJob, @Nullable Consumer<DiscoveryProgress> observer) {
        discover(discoveryJob, true, observer);
    }

    public void discover(DiscoveryJob discoveryJob, boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer) {
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignored = jobSynchronizer.registerDiscoveryTask()) {
            discoverArtists(discoveryJob, cacheEnabled, observer);
        }
    }

    private void discoverArtists(DiscoveryJob discoveryJob, boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer) {

        progressDiscovery(FULL_PLANNING, null, observer);
        jobSynchronizer.interruptDiscoveryIfCancelled();
        List<Artist> artists = artistRepository.findAll(Sort.by("id")).stream().sorted().toList();
        List<DiscoveryPlan> plans = new ArrayList<>();
        for (Artist artist : artists) {
            jobSynchronizer.interruptDiscoveryIfCancelled();
            List<Album> albums = albumRepository.findByArtistId(artist.getId()).stream().sorted().toList();
            plans.add(artistDiscoveryService.plan(artist, albums));
        }
        jobSynchronizer.interruptDiscoveryIfCancelled();
        long tasksComplete = plans.stream().mapToLong(DiscoveryPlan::tasksSkipped).sum();
        long tasksTotal = plans.stream().mapToLong(DiscoveryPlan::tasksTotal).sum();
        progressDiscovery(FULL_PROCESSING, DiscoveryProgress.Value.of(tasksComplete, tasksTotal), observer);

        for (DiscoveryPlan plan : plans) {
            jobSynchronizer.interruptDiscoveryIfCancelled();
            if (plan.tasksPending() == 0) {
                continue;
            }
            long artistProgressOffset = tasksComplete - plan.tasksSkipped();
            artistDiscoveryService.discover(discoveryJob, plan, cacheEnabled, observer == null ? null : progress -> {
                DiscoveryProgress.Value value = progress.getValue();
                if (value != null && value.getItemsTotal() > 0) {
                    progressDiscovery(FULL_PROCESSING,
                            DiscoveryProgress.Value.of(artistProgressOffset + value.getItemsComplete(), tasksTotal), observer);
                }
            });
            tasksComplete += plan.tasksPending();
            progressDiscovery(FULL_PROCESSING, DiscoveryProgress.Value.of(tasksComplete, tasksTotal), observer);
        }
        jobSynchronizer.interruptDiscoveryIfCancelled();
    }

    private void progressDiscovery(Step step, @Nullable DiscoveryProgress.Value value, @Nullable Consumer<DiscoveryProgress> observer) {
        notifyProgressObserver(new DiscoveryProgress(step, value), observer);
    }

    private void notifyProgressObserver(DiscoveryProgress discoveryProgress, @Nullable Consumer<DiscoveryProgress> handler) {
        if (handler != null) {
            try {
                handler.accept(discoveryProgress);
            } catch (Throwable t) {
                logger.error("Could not call discovery progress observer.", t);
            }
        }
    }
}
