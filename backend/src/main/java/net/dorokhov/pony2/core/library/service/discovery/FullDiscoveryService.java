package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.repository.AlbumDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_DISCOVERY;

@Service
public class FullDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final AlbumDiscoveryRepository albumDiscoveryRepository;
    private final ArtistDiscoveryRepository artistDiscoveryRepository;
    private final ArtistDiscoveryService artistDiscoveryService;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final ConfigService configService;

    public FullDiscoveryService(
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            AlbumDiscoveryRepository albumDiscoveryRepository,
            ArtistDiscoveryRepository artistDiscoveryRepository,
            ArtistDiscoveryService artistDiscoveryService,
            LibraryJobSynchronizer jobSynchronizer,
            ConfigService configService
    ) {
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.albumDiscoveryRepository = albumDiscoveryRepository;
        this.artistDiscoveryRepository = artistDiscoveryRepository;
        this.artistDiscoveryService = artistDiscoveryService;
        this.jobSynchronizer = jobSynchronizer;
        this.configService = configService;
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

        List<Artist> artists = artistRepository.findAll(Sort.by("id")).stream().sorted().toList();
        long tasksPerArtist = configService.get().llmEnabled() ? 2 : 0;
        long tasksComplete = 0;
        long tasksTotal = artists.size() * tasksPerArtist + albumRepository.count();

        notifyProgressObserver(new DiscoveryProgress(FULL_DISCOVERY,
                DiscoveryProgress.Value.of(0, tasksTotal)), observer);

        for (Artist artist : artists) {
            jobSynchronizer.interruptDiscoveryIfCancelled();
            List<Album> albums = albumRepository.findByArtistId(artist.getId()).stream().sorted().toList();
            boolean discoverArtist = shouldDiscoverArtist(artist, tasksPerArtist);
            long artistProgressOffset = tasksComplete;
            artistDiscoveryService.discoverForFullJob(discoveryJob, artist, cacheEnabled, albums, discoverArtist, album -> {
                jobSynchronizer.interruptDiscoveryIfCancelled();
                return shouldDiscoverAlbum(album);
            }, observer == null ? null : progress -> {
                DiscoveryProgress.Value value = progress.getValue();
                if (value != null && value.getItemsTotal() > 0) {
                    notifyProgressObserver(new DiscoveryProgress(FULL_DISCOVERY,
                            DiscoveryProgress.Value.of(artistProgressOffset + value.getItemsComplete(), tasksTotal)), observer);
                }
            });
            tasksComplete += tasksPerArtist + albums.size();
            notifyProgressObserver(new DiscoveryProgress(
                    FULL_DISCOVERY,
                    DiscoveryProgress.Value.of(tasksComplete, tasksTotal)
            ), observer);
        }
        jobSynchronizer.interruptDiscoveryIfCancelled();
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

    private boolean shouldDiscoverArtist(Artist artist, long tasksTotal) {
        return artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc(artist.getId())
                .map(discovery -> isDiscoveryOutdated(artist.getUpdateDate(), discovery.getCreationDate()) ||
                        artistDiscoveryRepository.countCompletedTasksByDiscoveryId(discovery.getId()) < tasksTotal)
                .orElse(true);
    }

    private boolean shouldDiscoverAlbum(Album album) {
        return albumDiscoveryRepository.findFirstByAlbumIdOrderByCreationDateDesc(album.getId())
                .map(discovery -> isDiscoveryOutdated(album.getUpdateDate(), discovery.getCreationDate()) ||
                        isDiscoveryOutdated(album.getArtist().getUpdateDate(), discovery.getCreationDate()) ||
                        albumDiscoveryRepository.countCompletedTasksByDiscoveryId(discovery.getId()) < 1)
                .orElse(true);
    }

    private boolean isDiscoveryOutdated(@Nullable LocalDateTime updateDate, @Nullable LocalDateTime discoveryDate) {
        return updateDate != null && (discoveryDate == null || updateDate.isAfter(discoveryDate));
    }
}
