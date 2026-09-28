package net.dorokhov.pony2.api.config.domain;

import jakarta.annotation.Nullable;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Objects.requireNonNull;

public record ConfigSet(
        @Nullable LocalDateTime updateDate,
        List<File> libraryFolders,
        @Nullable String llmUrl,
        @Nullable String llmModel,
        @Nullable String llmApiKey
) {

    public ConfigSet {
        libraryFolders = List.copyOf(requireNonNull(libraryFolders, "libraryFolders"));
    }

    public boolean llmEnabled() {
        return StringUtils.hasText(llmUrl) && StringUtils.hasText(llmModel);
    }
}
