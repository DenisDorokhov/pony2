package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.domain.ConfigDto;

public interface ConfigFacade {
    
    ConfigDto getConfig();
    
    ConfigDto saveConfig(ConfigDto config);
}
