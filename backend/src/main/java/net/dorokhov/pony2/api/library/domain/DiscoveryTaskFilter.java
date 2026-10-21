package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

public record DiscoveryTaskFilter(
        @Nullable Result result,
        @Nullable DiscoveryTaskType type
) {
    public enum Result {
        FAILED, NO_RESULT
    }
}
