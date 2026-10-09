package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_ALBUM_DISCOVERY;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.FULL_ARTIST_DISCOVERY;

@Service
public class FullDiscoveryService {

    private static final int ALBUM_PAGE_SIZE = 100;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final ArtistDiscoveryRepository artistDiscoveryRepository;
    private final AlbumDiscoveryRepository albumDiscoveryRepository;
    private final LibraryJobSynchronizer jobSynchronizer;

    public FullDiscoveryService(
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            ArtistDiscoveryRepository artistDiscoveryRepository,
            AlbumDiscoveryRepository albumDiscoveryRepository,
            LibraryJobSynchronizer jobSynchronizer
    ) {
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.artistDiscoveryRepository = artistDiscoveryRepository;
        this.albumDiscoveryRepository = albumDiscoveryRepository;
        this.jobSynchronizer = jobSynchronizer;
    }

    public void discover(DiscoveryJob discoveryJob, @Nullable Consumer<DiscoveryProgress> observer) {
        discover(discoveryJob, true, observer);
    }

    public void discover(DiscoveryJob discoveryJob, boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer) {
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignored = jobSynchronizer.registerDiscoveryTask()) {
            discoverArtists(observer);
            discoverAlbums(observer);
        }
    }

    private void discoverArtists(@Nullable Consumer<DiscoveryProgress> observer) {

        List<Artist> artists = artistRepository.findAll(Sort.by("id"));
        long artistsComplete = 0;
        long artistsTotal = artists.size();

        notifyProgressObserver(new DiscoveryProgress(FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(artistsComplete, artistsTotal)), observer);

        for (Artist artist : artists) {
            if (shouldDiscoverArtist(artist)) {
                try {
                    jobSynchronizer.interruptDiscoveryIfCancelled();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            artistsComplete++;
            notifyProgressObserver(new DiscoveryProgress(
                    FULL_ARTIST_DISCOVERY,
                    DiscoveryProgress.Value.of(artistsComplete, artistsTotal)
            ), observer);
        }
    }

    private void discoverAlbums(@Nullable Consumer<DiscoveryProgress> observer) {

        Page<Album> albums;
        Pageable pageable = PageRequest.of(0, ALBUM_PAGE_SIZE, Sort.by("id"));
        long albumsComplete = 0;
        long albumsTotal = 0;
        boolean started = false;
        do {
            albums = albumRepository.findAll(pageable);
            if (!started) {
                albumsTotal = albums.getTotalElements();
                notifyProgressObserver(new DiscoveryProgress(FULL_ALBUM_DISCOVERY, DiscoveryProgress.Value.of(albumsComplete, albumsTotal)), observer);
                started = true;
            }
            for (Album album : albums.getContent()) {
                if (shouldDiscoverAlbum(album)) {
                    try {
                        Thread.sleep(10);
                        jobSynchronizer.interruptDiscoveryIfCancelled();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
                albumsComplete++;
                notifyProgressObserver(new DiscoveryProgress(
                        FULL_ALBUM_DISCOVERY,
                        DiscoveryProgress.Value.of(albumsComplete, albumsTotal)
                ), observer);
            }
            pageable = albums.nextPageable();
        } while (albums.hasNext());
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

    private boolean shouldDiscoverAlbum(Album album) {
        return albumDiscoveryRepository.findFirstByAlbumIdOrderByCreationDateDesc(album.getId())
                .map(discovery -> isDiscoveryOutdated(album.getUpdateDate(), discovery.getCreationDate()))
                .orElse(true);
    }

    private boolean isDiscoveryOutdated(@Nullable LocalDateTime updateDate, @Nullable LocalDateTime discoveryDate) {
        return updateDate != null && (discoveryDate == null || updateDate.isAfter(discoveryDate));
    }
}
