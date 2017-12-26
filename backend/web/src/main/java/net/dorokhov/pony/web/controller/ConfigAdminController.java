package net.dorokhov.pony.web.controller;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.dorokhov.pony.web.domain.ConfigDto;
import net.dorokhov.pony.web.service.ConfigFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/config", produces = "application/json")
@Api(tags = "Config Administration")
public class ConfigAdminController implements ErrorHandlingController {
    
    private final ConfigFacade configFacade;

    public ConfigAdminController(ConfigFacade configFacade) {
        this.configFacade = configFacade;
    }

    @GetMapping
    @ApiOperation("Get configuration.")
    public ConfigDto getConfig() {
        return configFacade.getConfig();
    }
    
    @PutMapping
    @ApiOperation("Update configuration.")
    public ConfigDto saveConfig(@Valid @RequestBody ConfigDto config) {
        return configFacade.saveConfig(config);
    }
}
