package net.dorokhov.pony2.core.library.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ARTIST_DISCOVERY;

@Service
public class ArtistDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void discover(DiscoveryJob discoveryJob, Artist artist, @Nullable Consumer<DiscoveryProgress> observer) {
        notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, null), observer);
        try {
            Thread.sleep(100);
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
