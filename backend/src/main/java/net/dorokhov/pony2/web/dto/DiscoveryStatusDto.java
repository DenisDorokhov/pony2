package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;

public final class DiscoveryStatusDto {

    private boolean discoveryEnabled;
    private DiscoveryJobProgressDto progress;

    public boolean isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public DiscoveryStatusDto setDiscoveryEnabled(boolean discoveryEnabled) {
        this.discoveryEnabled = discoveryEnabled;
        return this;
    }

    @Nullable
    public DiscoveryJobProgressDto getProgress() {
        return progress;
    }

    public DiscoveryStatusDto setProgress(@Nullable DiscoveryJobProgressDto progress) {
        this.progress = progress;
        return this;
    }

    public static DiscoveryStatusDto of(boolean discoveryEnabled, @Nullable DiscoveryJobProgressDto progress) {
        return new DiscoveryStatusDto()
                .setDiscoveryEnabled(discoveryEnabled)
                .setProgress(progress);
    }
}
