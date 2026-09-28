package net.dorokhov.pony2.core.config.service;

import net.dorokhov.pony2.api.config.domain.Config;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.config.repository.ConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

@Service
public class ConfigServiceImpl implements ConfigService {

    static final String CONFIG_LIBRARY_FOLDERS = "libraryFolders";
    static final String CONFIG_LLM_URL = "llmUrl";
    static final String CONFIG_LLM_MODEL = "llmModel";
    static final String CONFIG_LLM_API_KEY = "llmApiKey";

    private final ConfigRepository configRepository;

    public ConfigServiceImpl(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigSet get() {
        List<Config> configs = configRepository.findAll();
        Map<String, String> values = new HashMap<>();
        for (Config config : configs) {
            values.put(config.getId(), config.getValue());
        }
        LocalDateTime updateDate = configs.stream()
                .map(config -> config.getUpdateDate() != null ? config.getUpdateDate() : config.getCreationDate())
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return new ConfigSet(
                updateDate,
                readLibraryFolders(values.get(CONFIG_LIBRARY_FOLDERS)),
                values.get(CONFIG_LLM_URL),
                values.get(CONFIG_LLM_MODEL),
                values.get(CONFIG_LLM_API_KEY)
        );
    }

    private List<File> readLibraryFolders(String value) {
        if (value == null) {
            return emptyList();
        }
        return JsonConverter.listFromJson(value, String.class)
                .stream()
                .map(File::new)
                .toList();
    }

    @Override
    @Transactional
    public void update(ConfigSetUpdateCommand command) {
        validate(command);
        saveLibraryFolders(command.getLibraryFolders());
        saveStringConfig(CONFIG_LLM_URL, command.getLlmUrl());
        saveStringConfig(CONFIG_LLM_MODEL, command.getLlmModel());
        saveStringConfig(CONFIG_LLM_API_KEY, command.getLlmApiKey());
    }

    private void validate(ConfigSetUpdateCommand command) {
        requireNonNull(command, "command");
        if (!StringUtils.hasText(command.getLlmUrl()) && StringUtils.hasText(command.getLlmModel())) {
            throw new IllegalArgumentException("LLM URL must be configured when LLM model is configured.");
        }
        if (!StringUtils.hasText(command.getLlmUrl()) && StringUtils.hasText(command.getLlmApiKey())) {
            throw new IllegalArgumentException("LLM URL must be configured when LLM API key is configured.");
        }
        if (StringUtils.hasText(command.getLlmUrl()) && !StringUtils.hasText(command.getLlmModel())) {
            throw new IllegalArgumentException("LLM model must be configured when LLM URL is configured.");
        }
    }

    private void saveLibraryFolders(List<File> files) {
        String value = JsonConverter.toJson(files.stream()
                .map(File::getPath)
                .toList());
        saveStringConfig(CONFIG_LIBRARY_FOLDERS, value);
    }

    private void saveStringConfig(String id, String value) {
        Config config = configRepository.findById(id)
                .orElseGet(() -> new Config().setId(id));
        config.setValue(StringUtils.hasText(value) ? value : null);
        configRepository.save(config);
    }
}
