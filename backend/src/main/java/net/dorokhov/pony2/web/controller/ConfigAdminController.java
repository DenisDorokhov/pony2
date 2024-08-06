package net.dorokhov.pony2.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony2.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony2.web.dto.ConfigDto;
import net.dorokhov.pony2.web.service.ConfigFacade;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class ConfigAdminController implements ErrorHandlingController {

    private final ConfigFacade configFacade;

    public ConfigAdminController(ConfigFacade configFacade) {
        this.configFacade = configFacade;
    }

    @GetMapping("/api/admin/config")
    public ConfigDto getConfig() {
        return configFacade.getConfig();
    }

    @PutMapping("/api/admin/config")
    public ConfigDto saveConfig(@Valid @RequestBody ConfigDto config) {
        return configFacade.saveConfig(config);
    }
}
