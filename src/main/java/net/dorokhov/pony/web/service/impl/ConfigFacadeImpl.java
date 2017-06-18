package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.web.domain.ConfigDto;
import net.dorokhov.pony.web.domain.LibraryFolderDto;
import net.dorokhov.pony.web.service.ConfigFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class ConfigFacadeImpl implements ConfigFacade {

    private final ConfigService configService;

    public ConfigFacadeImpl(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigDto getConfig() {
        return new ConfigDto(configService.getAutoScanInterval(),
                configService.getLibraryFolders().stream()
                        .map(LibraryFolderDto::of)
                        .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public ConfigDto saveConfig(ConfigDto config) {
        configService.saveAutoScanInterval(config.getAutoScanInterval());
        configService.saveLibraryFolders(config.getLibraryFolders().stream()
                .map(LibraryFolderDto::convert)
                .collect(Collectors.toList()));
        return getConfig();
    }
}
