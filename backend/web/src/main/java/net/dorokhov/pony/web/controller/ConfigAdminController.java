package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.domain.ConfigDto;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.service.ConfigFacade;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static net.dorokhov.pony.web.controller.common.ApiResponseValues.*;

@RestController
@RequestMapping(value = "/api/admin/config", produces = "application/json")
@Api(tags = "Config Administration")
@ApiResponses({
        @ApiResponse(code = UNAUTHORIZED_CODE, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
        @ApiResponse(code = FORBIDDEN_CODE, message = FORBIDDEN_MESSAGE, response = ErrorDto.class),
})
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
    @ApiResponses({
            @ApiResponse(code = BAD_REQUEST_CODE, message = BAD_REQUEST_MESSAGE, response = ErrorDto.class),
    })
    public ConfigDto saveConfig(@Valid @RequestBody ConfigDto config) {
        return configFacade.saveConfig(config);
    }
}
