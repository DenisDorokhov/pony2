package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryJobProgress;

public final class DiscoveryJobProgressDto {

    private DiscoveryJobDto discoveryJob;
    private DiscoveryProgressDto discoveryProgress;

    public DiscoveryJobDto getDiscoveryJob() {
        return discoveryJob;
    }

    public DiscoveryJobProgressDto setDiscoveryJob(DiscoveryJobDto discoveryJob) {
        this.discoveryJob = discoveryJob;
        return this;
    }

    @Nullable
    public DiscoveryProgressDto getDiscoveryProgress() {
        return discoveryProgress;
    }

    public DiscoveryJobProgressDto setDiscoveryProgress(@Nullable DiscoveryProgressDto discoveryProgress) {
        this.discoveryProgress = discoveryProgress;
        return this;
    }

    public static DiscoveryJobProgressDto of(DiscoveryJobProgress discoveryJobProgress) {
        return new DiscoveryJobProgressDto()
                .setDiscoveryJob(DiscoveryJobDto.of(discoveryJobProgress.getDiscoveryJob()))
                .setDiscoveryProgress(discoveryJobProgress.getDiscoveryProgress() != null
                        ? DiscoveryProgressDto.of(discoveryJobProgress.getDiscoveryProgress()) : null);
    }
}
