package net.dorokhov.pony.config.service.impl;

import com.google.common.base.Strings;
import net.dorokhov.pony.config.domain.Config;
import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.config.repository.ConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements ConfigService {

    static final String CONFIG_AUTO_SCAN_INTERVAL = "autoScanInterval";

    static final String CONFIG_LIBRARY_FOLDERS = "libraryFolders";
    static final String CONFIG_LIBRARY_FOLDERS_SEPARATOR = ":::";

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
                .map(Config::builder).orElse(Config.builder().id(CONFIG_AUTO_SCAN_INTERVAL));
        builder.value(value);
        configRepository.save(builder.build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<File> fetchLibraryFolders() {
        List<String> paths = Optional.ofNullable(configRepository.findOne(CONFIG_LIBRARY_FOLDERS))
                .map(Config::getValue)
                .map(s -> s.split(CONFIG_LIBRARY_FOLDERS_SEPARATOR))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
        return paths.stream()
                .map(String::trim)
                .filter(s -> s.length() > 0)
                .map(File::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveLibraryFolders(List<File> files) {

        String value = files.stream()
                .map(File::getPath)
                .collect(Collectors.joining(CONFIG_LIBRARY_FOLDERS_SEPARATOR));

        Config.Builder builder = Optional.ofNullable(configRepository.findOne(CONFIG_LIBRARY_FOLDERS))
                .map(Config::builder).orElse(Config.builder().id(CONFIG_LIBRARY_FOLDERS));
        builder.value(Strings.emptyToNull(value));
        configRepository.save(builder.build());
    }
}
