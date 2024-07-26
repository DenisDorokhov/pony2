package net.dorokhov.pony3.web.dto;

import jakarta.validation.Valid;

import java.io.File;
import java.util.List;

public final class ConfigDto {

    @Valid
    private List<LibraryFolderDto> libraryFolders;

    public List<LibraryFolderDto> getLibraryFolders() {
        return libraryFolders;
    }

    public ConfigDto setLibraryFolders(@Valid List<LibraryFolderDto> libraryFolders) {
        this.libraryFolders = libraryFolders;
        return this;
    }

    public static ConfigDto of(List<File> libraryFolders) {
        return new ConfigDto()
                .setLibraryFolders(libraryFolders.stream()
                        .map(LibraryFolderDto::of)
                        .toList());
    }
}
