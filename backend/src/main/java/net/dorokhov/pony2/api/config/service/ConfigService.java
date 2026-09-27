package net.dorokhov.pony2.api.config.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConfigService {

    Optional<LocalDateTime> getUpdateDate();

    List<File> getLibraryFolders();

    void saveLibraryFolders(List<File> libraryFolders);

    Optional<String> getLlmUrl();

    void saveLlmUrl(String llmUrl);

    Optional<String> getLlmApiKey();

    void saveLlmApiKey(String llmApiKey);
}
