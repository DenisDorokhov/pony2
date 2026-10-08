package net.dorokhov.pony2.web.dto;

import jakarta.validation.Valid;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.web.validation.ValidLlmConfig;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@ValidLlmConfig
public final class ConfigDto {

    private LocalDateTime updateDate;

    private List<@Valid LibraryFolderDto> libraryFolders;

    private String llmUrl;
    private String llmModel;
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

    public ConfigDto setLibraryFolders(List<@Valid LibraryFolderDto> libraryFolders) {
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

    public String getLlmModel() {
        return llmModel;
    }

    public ConfigDto setLlmModel(String llmModel) {
        this.llmModel = llmModel;
        return this;
    }

    public String getLlmApiKey() {
        return llmApiKey;
    }

    public ConfigDto setLlmApiKey(String llmApiKey) {
        this.llmApiKey = llmApiKey;
        return this;
    }

    public ConfigSetUpdateCommand convert() {
        return new ConfigSetUpdateCommand()
                .setLibraryFolders(libraryFolders != null ? libraryFolders.stream()
                        .map(folder -> new File(folder.getPath()))
                        .toList() : List.of())
                .setLlmUrl(llmUrl)
                .setLlmModel(llmModel)
                .setLlmApiKey(llmApiKey);
    }

    public static ConfigDto of(ConfigSet configSet) {
        return new ConfigDto()
                .setUpdateDate(configSet.updateDate())
                .setLibraryFolders(configSet.libraryFolders().stream()
                        .map(LibraryFolderDto::of)
                        .toList())
                .setLlmUrl(configSet.llmUrl())
                .setLlmModel(configSet.llmModel())
                .setLlmApiKey(configSet.llmApiKey());
    }
}
