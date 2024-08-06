package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.web.dto.ConfigDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Service
public class ConfigFacade {

    private final ConfigService configService;

    public ConfigFacade(ConfigService configService) {
        this.configService = configService;
    }

    @Transactional(readOnly = true)
    public ConfigDto getConfig() {
        return ConfigDto.of(configService.getUpdateDate().orElse(null), configService.getLibraryFolders());
    }

    @Transactional
    public ConfigDto saveConfig(ConfigDto config) {
        configService.saveLibraryFolders(config.getLibraryFolders().stream()
                .map(folder -> new File(folder.getPath()))
                .toList());
        return getConfig();
    }
}
