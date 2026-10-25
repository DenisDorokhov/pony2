package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Limit;
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
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final ArtistDiscoveryService artistDiscoveryService;
    private final LibraryJobSynchronizer jobSynchronizer;

    public FullDiscoveryService(
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            ArtistDiscoveryService artistDiscoveryService,
            LibraryJobSynchronizer jobSynchronizer
    ) {
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
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

        List<Artist> artists = artistRepository.findAll(Sort.by("id")).stream().sorted().toList();
        long tasksPerArtist = 2;
        long tasksComplete = 0;
        long tasksTotal = artists.size() * tasksPerArtist + albumRepository.count();

        notifyProgressObserver(new DiscoveryProgress(FULL_DISCOVERY,
                DiscoveryProgress.Value.of(0, tasksTotal)), observer);

        for (Artist artist : artists) {
            jobSynchronizer.interruptDiscoveryIfCancelled();
            List<Album> albums = albumRepository.findByArtistId(artist.getId()).stream().sorted().toList();
            boolean discoverArtist = shouldDiscoverArtist(artist);
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
            } catch (Throwable t) {
                logger.error("Could not call discovery progress observer.", t);
            }
        }
    }

    private boolean shouldDiscoverArtist(Artist artist) {
        LocalDateTime entityDate = artist.getUpdateDate() != null ? artist.getUpdateDate() : artist.getCreationDate();
        return discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                        artist.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1))
                .map(entityDate::isAfter).orElse(true) ||
                discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                                artist.getId(), DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1))
                        .map(entityDate::isAfter).orElse(true);
    }

    private boolean shouldDiscoverAlbum(Album album) {
        LocalDateTime entityDate = album.getUpdateDate() != null ? album.getUpdateDate() : album.getCreationDate();
        return discoveryTaskRepository.findLatestCompletedCreationDateByAlbumIdAndType(
                        album.getId(), DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, Limit.of(1))
                .map(entityDate::isAfter).orElse(true);
    }
}
