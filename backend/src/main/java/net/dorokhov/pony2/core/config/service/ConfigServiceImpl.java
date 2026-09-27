package net.dorokhov.pony2.core.config.service;

import com.google.common.base.Strings;
import net.dorokhov.pony2.api.config.domain.Config;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.config.repository.ConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Service
public class ConfigServiceImpl implements ConfigService {

    static final String CONFIG_LIBRARY_FOLDERS = "libraryFolders";
    static final String CONFIG_LLM_URL = "llmUrl";
    static final String CONFIG_LLM_API_KEY = "llmApiKey";

    private final ConfigRepository configRepository;

    public ConfigServiceImpl(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public Optional<LocalDateTime> getUpdateDate() {
        return configRepository.findAll().stream()
                .map(config -> config.getUpdateDate() != null ? config.getUpdateDate() : config.getCreationDate())
                .max(LocalDateTime::compareTo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<File> getLibraryFolders() {
        return configRepository.findById(CONFIG_LIBRARY_FOLDERS)
                .map(Config::getValue)
                .map(s -> JsonConverter.listFromJson(s, String.class))
                .orElse(emptyList())
                .stream()
                .map(File::new)
                .toList();
    }

    @Override
    @Transactional
    public void saveLibraryFolders(List<File> files) {
        String value = JsonConverter.toJson(files.stream()
                .map(File::getPath)
                .toList());
        saveStringConfig(CONFIG_LIBRARY_FOLDERS, value);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getLlmUrl() {
        return getStringConfig(CONFIG_LLM_URL);
    }

    @Override
    @Transactional
    public void saveLlmUrl(String llmUrl) {
        saveStringConfig(CONFIG_LLM_URL, llmUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getLlmApiKey() {
        return getStringConfig(CONFIG_LLM_API_KEY);
    }

    @Override
    @Transactional
    public void saveLlmApiKey(String llmApiKey) {
        saveStringConfig(CONFIG_LLM_API_KEY, llmApiKey);
    }

    private Optional<String> getStringConfig(String id) {
        return configRepository.findById(id)
                .map(Config::getValue)
                .filter(value -> !Strings.isNullOrEmpty(value));
    }

    private void saveStringConfig(String id, String value) {
        Config config = configRepository.findById(id)
                .orElseGet(() -> new Config().setId(id));
        config.setValue(Strings.emptyToNull(value));
        configRepository.save(config);
    }
}
