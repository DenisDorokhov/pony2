package net.dorokhov.pony2.api.config.service;

import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;

public interface ConfigService {

    ConfigSet get();

    void update(ConfigSetUpdateCommand command);
}
