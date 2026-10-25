package net.dorokhov.pony2.api.config.domain;

import jakarta.annotation.Nullable;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Objects.requireNonNull;

public record ConfigSet(
        @Nullable LocalDateTime updateDate,
        List<File> libraryFolders,
        boolean discoveryEnabled,
        @Nullable String llmUrl,
        @Nullable String llmModel,
        @Nullable String llmApiKey
) {

    public ConfigSet {
        libraryFolders = List.copyOf(requireNonNull(libraryFolders, "libraryFolders"));
    }
}
