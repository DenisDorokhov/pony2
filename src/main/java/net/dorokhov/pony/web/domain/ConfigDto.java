package net.dorokhov.pony.web.domain;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public final class ConfigDto {

    private final Integer autoScanInterval;

    @Valid
    private final List<LibraryFolderDto> libraryFolders;

    public ConfigDto(@Nullable Integer autoScanInterval, List<LibraryFolderDto> libraryFolders) {
        this.autoScanInterval = autoScanInterval;
        this.libraryFolders = unmodifiableList(libraryFolders);
    }

    public Integer getAutoScanInterval() {
        return autoScanInterval;
    }

    public List<LibraryFolderDto> getLibraryFolders() {
        return libraryFolders;
    }

    public static ConfigDto of(@Nullable Integer autoScanInterval, List<File> libraryFolders) {
        return new ConfigDto(autoScanInterval,
                libraryFolders.stream()
                        .map(LibraryFolderDto::of)
                        .collect(Collectors.toList()));
    }
}
