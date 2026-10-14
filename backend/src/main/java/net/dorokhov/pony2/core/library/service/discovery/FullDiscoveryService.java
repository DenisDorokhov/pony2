package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_ARTIST_DISCOVERY;

@Service
public class FullDiscoveryService {

    private static final int ARTIST_PROGRESS_UNITS = 100;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtistRepository artistRepository;
    private final ArtistDiscoveryRepository artistDiscoveryRepository;
    private final ArtistDiscoveryService artistDiscoveryService;
    private final LibraryJobSynchronizer jobSynchronizer;

    public FullDiscoveryService(
            ArtistRepository artistRepository,
            ArtistDiscoveryRepository artistDiscoveryRepository,
            ArtistDiscoveryService artistDiscoveryService,
            LibraryJobSynchronizer jobSynchronizer
    ) {
        this.artistRepository = artistRepository;
        this.artistDiscoveryRepository = artistDiscoveryRepository;
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

        List<Artist> artists = artistRepository.findAll(Sort.by("id"));
        long artistsComplete = 0;
        long artistsTotal = artists.size();

        notifyProgressObserver(new DiscoveryProgress(FULL_ARTIST_DISCOVERY,
                DiscoveryProgress.Value.of(0, artistsTotal * ARTIST_PROGRESS_UNITS)), observer);

        for (Artist artist : artists) {
            if (shouldDiscoverArtist(artist)) {
                jobSynchronizer.interruptDiscoveryIfCancelled();
                long artistProgressOffset = artistsComplete * ARTIST_PROGRESS_UNITS;
                artistDiscoveryService.discover(discoveryJob, artist, cacheEnabled, observer == null ? null : progress -> {
                    DiscoveryProgress.Value value = progress.getValue();
                    if (value != null && value.getItemsTotal() > 0) {
                        // Give each artist an equal share of the overall progress.
                        long artistProgress = value.getItemsComplete() * ARTIST_PROGRESS_UNITS / value.getItemsTotal();
                        notifyProgressObserver(new DiscoveryProgress(FULL_ARTIST_DISCOVERY,
                                DiscoveryProgress.Value.of(artistProgressOffset + artistProgress,
                                        artistsTotal * ARTIST_PROGRESS_UNITS)), observer);
                    }
                });
            }
            artistsComplete++;
            notifyProgressObserver(new DiscoveryProgress(
                    FULL_ARTIST_DISCOVERY,
                    DiscoveryProgress.Value.of(artistsComplete * ARTIST_PROGRESS_UNITS, artistsTotal * ARTIST_PROGRESS_UNITS)
            ), observer);
        }
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

    private boolean shouldDiscoverArtist(Artist artist) {
        return artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc(artist.getId())
                .map(discovery -> isDiscoveryOutdated(artist.getUpdateDate(), discovery.getCreationDate()))
                .orElse(true);
    }

    private boolean isDiscoveryOutdated(@Nullable LocalDateTime updateDate, @Nullable LocalDateTime discoveryDate) {
        return updateDate != null && (discoveryDate == null || updateDate.isAfter(discoveryDate));
    }
}
