package net.dorokhov.pony2.test;

import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;

import java.time.LocalDateTime;

public final class DiscoveryJobFixtures {

    private DiscoveryJobFixtures() {
    }

    public static DiscoveryJob discoveryJobFull() {
        return discoveryJob(DiscoveryType.FULL);
    }

    public static DiscoveryJob discoveryJobArtist() {
        return discoveryJob(DiscoveryType.ARTIST);
    }

    public static DiscoveryJob discoveryJobAlbum() {
        return discoveryJob(DiscoveryType.ALBUM);
    }

    public static DiscoveryJob discoveryJob(DiscoveryType discoveryType) {
        return new DiscoveryJob()
                .setCreationDate(LocalDateTime.now())
                .setUpdateDate(LocalDateTime.now())
                .setStatus(DiscoveryJob.Status.STARTING)
                .setType(discoveryType);
    }
}
