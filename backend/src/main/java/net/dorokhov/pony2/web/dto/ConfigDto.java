package net.dorokhov.pony2.web.dto;

import jakarta.validation.Valid;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public final class ConfigDto {

    private LocalDateTime updateDate;

    @Valid
    private List<LibraryFolderDto> libraryFolders;

    private String llmUrl;
    private String llmApiKey;

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

    public String getLlmUrl() {
        return llmUrl;
    }

    public ConfigDto setLlmUrl(String llmUrl) {
        this.llmUrl = llmUrl;
        return this;
    }

    public String getLlmApiKey() {
        return llmApiKey;
    }

    public ConfigDto setLlmApiKey(String llmApiKey) {
        this.llmApiKey = llmApiKey;
        return this;
    }

    public static ConfigDto of(LocalDateTime updateDate, List<File> libraryFolders, String llmUrl, String llmApiKey) {
        return new ConfigDto()
                .setUpdateDate(updateDate)
                .setLibraryFolders(libraryFolders.stream()
                        .map(LibraryFolderDto::of)
                        .toList())
                .setLlmUrl(llmUrl)
                .setLlmApiKey(llmApiKey);
    }
}
