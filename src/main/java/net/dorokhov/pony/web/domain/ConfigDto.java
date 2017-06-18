package net.dorokhov.pony.web.domain;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.util.List;

public final class ConfigDto {
    
    private final Integer autoScanInterval;
    
    @Valid
    private final List<LibraryFolderDto> libraryFolders;

    public ConfigDto(@Nullable Integer autoScanInterval, List<LibraryFolderDto> libraryFolders) {
        this.autoScanInterval = autoScanInterval;
        this.libraryFolders = ImmutableList.copyOf(libraryFolders);
    }

    public Integer getAutoScanInterval() {
        return autoScanInterval;
    }

    public List<LibraryFolderDto> getLibraryFolders() {
        return libraryFolders;
    }
}
