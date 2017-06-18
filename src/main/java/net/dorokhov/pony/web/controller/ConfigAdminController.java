package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.web.domain.ConfigDto;
import net.dorokhov.pony.web.service.ConfigFacade;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/config")
public class ConfigAdminController implements ErrorHandlingController {
    
    private final ConfigFacade configFacade;

    public ConfigAdminController(ConfigFacade configFacade) {
        this.configFacade = configFacade;
    }

    @GetMapping
    public ConfigDto getConfig() {
        return configFacade.getConfig();
    }
    
    @PutMapping
    public ConfigDto saveConfig(@Valid @RequestBody ConfigDto config) {
        return configFacade.saveConfig(config);
    }
}
