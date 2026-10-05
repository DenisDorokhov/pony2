package net.dorokhov.pony2.core.library.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
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
import java.util.concurrent.atomic.AtomicReference;
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
    private final ArtistDiscoveryService artistDiscoveryService;
    private final AlbumDiscoveryService albumDiscoveryService;

    public FullDiscoveryService(
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            ArtistDiscoveryRepository artistDiscoveryRepository,
            AlbumDiscoveryRepository albumDiscoveryRepository,
            ArtistDiscoveryService artistDiscoveryService,
            AlbumDiscoveryService albumDiscoveryService
    ) {
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.artistDiscoveryRepository = artistDiscoveryRepository;
        this.albumDiscoveryRepository = albumDiscoveryRepository;
        this.artistDiscoveryService = artistDiscoveryService;
        this.albumDiscoveryService = albumDiscoveryService;
    }

    public void discover(DiscoveryJob discoveryJob, @Nullable Consumer<DiscoveryProgress> observer) {

        discoverArtists(discoveryJob, observer);

        discoverAlbums(discoveryJob, observer);
    }

    private void discoverArtists(DiscoveryJob discoveryJob, @Nullable Consumer<DiscoveryProgress> observer) {

        List<Artist> artists = artistRepository.findAll(Sort.by("id"));
        long artistsComplete = 0;
        long artistsTotal = artists.size();

        notifyProgressObserver(new DiscoveryProgress(FULL_ARTIST_DISCOVERY, DiscoveryProgress.Value.of(artistsComplete, artistsTotal)), observer);

        for (Artist artist : artists) {
            long initialArtistsComplete = artistsComplete;
            AtomicReference<Long> tasksTotal = new AtomicReference<>();
            if (shouldDiscoverArtist(artist)) {
                artistDiscoveryService.discover(discoveryJob, artist, discoveryProgress ->
                        notifyProgressObserver(
                                fullDiscoveryProgress(
                                        FULL_ARTIST_DISCOVERY,
                                        initialArtistsComplete,
                                        artistsTotal,
                                        discoveryProgress,
                                        tasksTotal
                                ),
                                observer
                        ));
            }
            artistsComplete++;
            notifyProgressObserver(new DiscoveryProgress(
                    FULL_ARTIST_DISCOVERY,
                    fullDiscoveryProgressValue(initialArtistsComplete, artistsComplete, artistsTotal, tasksTotal.get())
            ), observer);
        }
    }

    private void discoverAlbums(DiscoveryJob discoveryJob, @Nullable Consumer<DiscoveryProgress> observer) {

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
                long initialAlbumsComplete = albumsComplete;
                long finalAlbumsTotal = albumsTotal;
                AtomicReference<Long> tasksTotal = new AtomicReference<>();
                if (shouldDiscoverAlbum(album)) {
                    albumDiscoveryService.discover(discoveryJob, album, discoveryProgress ->
                            notifyProgressObserver(
                                    fullDiscoveryProgress(
                                            FULL_ALBUM_DISCOVERY,
                                            initialAlbumsComplete,
                                            finalAlbumsTotal,
                                            discoveryProgress,
                                            tasksTotal
                                    ),
                                    observer
                            ));
                }
                albumsComplete++;
                notifyProgressObserver(new DiscoveryProgress(
                        FULL_ALBUM_DISCOVERY,
                        fullDiscoveryProgressValue(initialAlbumsComplete, albumsComplete, albumsTotal, tasksTotal.get())
                ), observer);
            }
            pageable = albums.nextPageable();
        } while (albums.hasNext());
    }

    private DiscoveryProgress fullDiscoveryProgress(
            DiscoveryProgress.Step step,
            long initialItemsComplete,
            long itemsTotal,
            DiscoveryProgress discoveryProgress,
            AtomicReference<Long> tasksTotal
    ) {
        DiscoveryProgress.Value value = discoveryProgress.getValue();
        if (value == null) {
            return new DiscoveryProgress(step, DiscoveryProgress.Value.of(initialItemsComplete, itemsTotal));
        }
        tasksTotal.set(value.getItemsTotal());
        return new DiscoveryProgress(step, DiscoveryProgress.Value.of(
                initialItemsComplete * value.getItemsTotal() + value.getItemsComplete(),
                itemsTotal * value.getItemsTotal()
        ));
    }

    private DiscoveryProgress.Value fullDiscoveryProgressValue(
            long initialItemsComplete,
            long itemsComplete,
            long itemsTotal,
            @Nullable Long tasksTotal
    ) {
        if (tasksTotal == null) {
            return DiscoveryProgress.Value.of(itemsComplete, itemsTotal);
        }
        return DiscoveryProgress.Value.of(
                initialItemsComplete * tasksTotal + tasksTotal,
                itemsTotal * tasksTotal
        );
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
