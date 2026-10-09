package net.dorokhov.pony2.api.config.domain;

import static java.util.Objects.requireNonNull;

public record ConfigChangeEvent(ConfigSet oldConfig, ConfigSet newConfig) {
    public ConfigChangeEvent {
        requireNonNull(oldConfig, "oldConfig");
        requireNonNull(newConfig, "newConfig");
    }
}
