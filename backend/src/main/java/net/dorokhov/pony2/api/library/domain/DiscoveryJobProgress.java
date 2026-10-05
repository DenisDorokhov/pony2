package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;

public final class DiscoveryJobProgress {

    private final DiscoveryJob discoveryJob;
    private final DiscoveryProgress discoveryProgress;

    public DiscoveryJobProgress(DiscoveryJob discoveryJob, @Nullable DiscoveryProgress discoveryProgress) {
        this.discoveryJob = discoveryJob;
        this.discoveryProgress = discoveryProgress;
    }

    public DiscoveryJob getDiscoveryJob() {
        return discoveryJob;
    }

    @Nullable
    public DiscoveryProgress getDiscoveryProgress() {
        return discoveryProgress;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("discoveryJob", discoveryJob)
                .add("discoveryProgress", discoveryProgress)
                .toString();
    }
}
