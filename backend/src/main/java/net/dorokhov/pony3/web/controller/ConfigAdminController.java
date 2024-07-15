package net.dorokhov.pony3.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony3.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony3.web.dto.ConfigDto;
import net.dorokhov.pony3.web.service.ConfigFacade;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/admin/config", produces = APPLICATION_JSON_VALUE)
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