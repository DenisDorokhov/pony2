package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ALBUM_DISCOVERY;

@Service
public class AlbumDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LibraryJobSynchronizer jobSynchronizer;

    public AlbumDiscoveryService(LibraryJobSynchronizer jobSynchronizer) {
        this.jobSynchronizer = jobSynchronizer;
    }

    public void discover(DiscoveryJob discoveryJob, Album album, @Nullable Consumer<DiscoveryProgress> observer) {
        discover(discoveryJob, album, true, observer);
    }

    public void discover(DiscoveryJob discoveryJob, Album album, boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer) {
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignored = jobSynchronizer.registerDiscoveryTask()) {
            notifyProgressObserver(new DiscoveryProgress(ALBUM_DISCOVERY, null), observer);
            Thread.sleep(10);
            jobSynchronizer.interruptDiscoveryIfCancelled();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
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
}
