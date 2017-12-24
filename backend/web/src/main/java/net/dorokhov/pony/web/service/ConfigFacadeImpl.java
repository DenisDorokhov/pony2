package net.dorokhov.pony.web.service;

import java.util.stream.Collectors;

import net.dorokhov.pony.api.config.service.ConfigService;
import net.dorokhov.pony.web.domain.ConfigDto;
import net.dorokhov.pony.web.domain.LibraryFolderDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigFacadeImpl implements ConfigFacade {

    private final ConfigService configService;

    public ConfigFacadeImpl(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigDto getConfig() {
        return ConfigDto.of(configService.getAutoScanInterval(), configService.getLibraryFolders());
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
