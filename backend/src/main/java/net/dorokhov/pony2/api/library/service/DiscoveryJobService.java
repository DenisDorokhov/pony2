package net.dorokhov.pony2.api.library.service;

import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryJobProgress;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DiscoveryJobService {

    interface Observer {
        void onDiscoveryJobStarting(DiscoveryJob discoveryJob);
        void onDiscoveryJobStarted(DiscoveryJob discoveryJob);
        void onDiscoveryJobProgress(DiscoveryJobProgress discoveryJobProgress);
        void onDiscoveryJobCompleting(DiscoveryJob discoveryJob);
        void onDiscoveryJobCompleted(DiscoveryJob discoveryJob);
        void onDiscoveryJobModerating(DiscoveryJob discoveryJob);
        void onDiscoveryJobModerated(DiscoveryJob discoveryJob);
        void onDiscoveryJobFailing(DiscoveryJob discoveryJob);
        void onDiscoveryJobFailed(DiscoveryJob discoveryJob);
        void onDiscoveryJobInterrupting(DiscoveryJob discoveryJob);
        void onDiscoveryJobInterrupted(DiscoveryJob discoveryJob);
    }

    void addObserver(Observer observer);
    void removeObserver(Observer observer);

    Optional<DiscoveryJobProgress> getCurrentDiscoveryJobProgress();

    Optional<DiscoveryJobProgress> getDiscoveryJobProgress(String id);

    Page<DiscoveryJob> getAll(Pageable pageable);

    Optional<DiscoveryJob> getById(String id);

    DiscoveryJob startFullJob() throws ConcurrentLibraryJobException;
    DiscoveryJob startFullJob(boolean cacheEnabled) throws ConcurrentLibraryJobException;

    DiscoveryJob startArtistJob(String artistId) throws ConcurrentLibraryJobException;
    DiscoveryJob startArtistJob(String artistId, boolean cacheEnabled) throws ConcurrentLibraryJobException;

    DiscoveryJob startAlbumJob(String albumId) throws ConcurrentLibraryJobException;
    DiscoveryJob startAlbumJob(String albumId, boolean cacheEnabled) throws ConcurrentLibraryJobException;
}
