package net.dorokhov.pony2.api.config.service.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ConfigSetUpdateCommand {

    private List<File> libraryFolders = new ArrayList<>();
    private String llmUrl;
    private String llmModel;
    private String llmApiKey;

    public List<File> getLibraryFolders() {
        if (libraryFolders == null) {
            libraryFolders = new ArrayList<>();
        }
        return libraryFolders;
    }

    public ConfigSetUpdateCommand setLibraryFolders(List<File> libraryFolders) {
        this.libraryFolders = libraryFolders;
        return this;
    }

    public String getLlmUrl() {
        return llmUrl;
    }

    public ConfigSetUpdateCommand setLlmUrl(String llmUrl) {
        this.llmUrl = llmUrl;
        return this;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public ConfigSetUpdateCommand setLlmModel(String llmModel) {
        this.llmModel = llmModel;
        return this;
    }

    public String getLlmApiKey() {
        return llmApiKey;
    }

    public ConfigSetUpdateCommand setLlmApiKey(String llmApiKey) {
        this.llmApiKey = llmApiKey;
        return this;
    }
}
