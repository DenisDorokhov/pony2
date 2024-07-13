package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.api.config.service.ConfigService;
import net.dorokhov.pony3.web.dto.ConfigDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.stream.Collectors;

@Service
public class ConfigFacade {

    private final ConfigService configService;

    public ConfigFacade(ConfigService configService) {
        this.configService = configService;
    }

    @Transactional(readOnly = true)
    public ConfigDto getConfig() {
        return ConfigDto.of(configService.getAutoScanInterval().orElse(null), configService.getLibraryFolders());
    }

    @Transactional
    public ConfigDto saveConfig(ConfigDto config) {
        configService.saveAutoScanInterval(config.getAutoScanInterval());
        configService.saveLibraryFolders(config.getLibraryFolders().stream()
                .map(folder -> new File(folder.getPath()))
                .collect(Collectors.toList()));
        return getConfig();
    }
}
