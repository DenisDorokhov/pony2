package net.dorokhov.pony.config.service.impl;

import com.google.common.base.Strings;
import net.dorokhov.pony.common.JsonConverter;
import net.dorokhov.pony.config.domain.Config;
import net.dorokhov.pony.config.repository.ConfigRepository;
import net.dorokhov.pony.config.service.ConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Service
public class ConfigServiceImpl implements ConfigService {

    static final String CONFIG_AUTO_SCAN_INTERVAL = "autoScanInterval";
    static final String CONFIG_LIBRARY_FOLDERS = "libraryFolders";

    private final ConfigRepository configRepository;

    public ConfigServiceImpl(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public Integer getAutoScanInterval() {
        Config config = configRepository.findOne(CONFIG_AUTO_SCAN_INTERVAL);
        return config != null ? config.getInteger() : null;
    }

    @Override
    @Transactional
    public void saveAutoScanInterval(@Nullable Integer value) {
        Config.Builder builder = Optional.ofNullable(configRepository.findOne(CONFIG_AUTO_SCAN_INTERVAL))
                .map(Config::builder)
                .orElse(Config.builder().id(CONFIG_AUTO_SCAN_INTERVAL));
        builder.value(value);
        configRepository.save(builder.build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<File> getLibraryFolders() {
        return Optional.ofNullable(configRepository.findOne(CONFIG_LIBRARY_FOLDERS))
                .map(Config::getValue)
                .map(s -> JsonConverter.listFromJson(s, String.class))
                .orElse(emptyList())
                .stream()
                .map(File::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveLibraryFolders(List<File> files) {
        String value = JsonConverter.toJson(files.stream()
                .map(File::getPath)
                .collect(Collectors.toList()));
        Config.Builder builder = Optional.ofNullable(configRepository.findOne(CONFIG_LIBRARY_FOLDERS))
                .map(Config::builder)
                .orElse(Config.builder().id(CONFIG_LIBRARY_FOLDERS));
        builder.value(Strings.emptyToNull(value));
        configRepository.save(builder.build());
    }
}
