package net.dorokhov.pony2.web.dto;

import jakarta.validation.Valid;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public final class ConfigDto {

    private LocalDateTime updateDate;

    @Valid
    private List<LibraryFolderDto> libraryFolders;

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public ConfigDto setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public List<LibraryFolderDto> getLibraryFolders() {
        return libraryFolders;
    }

    public ConfigDto setLibraryFolders(@Valid List<LibraryFolderDto> libraryFolders) {
        this.libraryFolders = libraryFolders;
        return this;
    }

    public static ConfigDto of(LocalDateTime updateDate, List<File> libraryFolders) {
        return new ConfigDto()
                .setUpdateDate(updateDate)
                .setLibraryFolders(libraryFolders.stream()
                        .map(LibraryFolderDto::of)
                        .toList());
    }
}
