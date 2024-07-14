package net.dorokhov.pony3.web.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;

import java.io.File;
import java.util.List;

public final class ConfigDto {

    private Integer autoScanInterval;

    @Valid
    private List<LibraryFolderDto> libraryFolders;

    @Nullable
    public Integer getAutoScanInterval() {
        return autoScanInterval;
    }

    public ConfigDto setAutoScanInterval(Integer autoScanInterval) {
        this.autoScanInterval = autoScanInterval;
        return this;
    }

    public List<LibraryFolderDto> getLibraryFolders() {
        return libraryFolders;
    }

    public ConfigDto setLibraryFolders(@Valid List<LibraryFolderDto> libraryFolders) {
        this.libraryFolders = libraryFolders;
        return this;
    }

    public static ConfigDto of(@Nullable Integer autoScanInterval, List<File> libraryFolders) {
        return new ConfigDto()
                .setAutoScanInterval(autoScanInterval)
                .setLibraryFolders(libraryFolders.stream()
                        .map(LibraryFolderDto::of)
                        .toList());
    }
}
