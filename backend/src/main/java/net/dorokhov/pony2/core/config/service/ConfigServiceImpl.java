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

    private final ConfigRepository configRepository;

    public ConfigServiceImpl(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public Optional<LocalDateTime> getUpdateDate() {
        return configRepository.findById(CONFIG_LIBRARY_FOLDERS)
                .map(config -> {
                    if (config.getUpdateDate() == null) {
                        return config.getCreationDate();
                    }
                    return config.getUpdateDate();
                });
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
        Config config = configRepository.findById(CONFIG_LIBRARY_FOLDERS)
                .orElseGet(() -> new Config().setId(CONFIG_LIBRARY_FOLDERS));
        config.setValue(Strings.emptyToNull(value));
        configRepository.save(config);
    }
}
