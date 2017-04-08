package net.dorokhov.pony.config;

import com.google.common.base.Strings;
import net.dorokhov.pony.entity.Config;
import net.dorokhov.pony.repository.ConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Optional<Integer> getAutoScanInterval() {
        return Optional.ofNullable(configRepository.findOne(CONFIG_AUTO_SCAN_INTERVAL))
                .flatMap(Config::getInteger);
    }

    @Override
    @Transactional
    public void saveAutoScanInterval(Integer value) {
        Config config = Optional.ofNullable(configRepository.findOne(CONFIG_AUTO_SCAN_INTERVAL))
                .orElse(new Config(CONFIG_AUTO_SCAN_INTERVAL));
        config.setValue(value);
        configRepository.save(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<File> fetchLibraryFolders() {
        List<String> paths = Optional.ofNullable(configRepository.findOne(CONFIG_LIBRARY_FOLDERS))
                .flatMap(Config::getValue)
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

        Config config = Optional.ofNullable(configRepository.findOne(CONFIG_LIBRARY_FOLDERS))
                .orElse(new Config(CONFIG_LIBRARY_FOLDERS));
        config.setValue(Strings.emptyToNull(value));
        configRepository.save(config);
    }
}
