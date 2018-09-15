package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.service.InstallationFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static javax.servlet.http.HttpServletResponse.*;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.FORBIDDEN_MESSAGE;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.UNAUTHORIZED_MESSAGE;

@RestController
@RequestMapping(value = "/api/admin/installation", produces = "application/json")
@Api(tags = "Installation Administration")
@ApiResponses({
        @ApiResponse(code = SC_UNAUTHORIZED, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
        @ApiResponse(code = SC_FORBIDDEN, message = FORBIDDEN_MESSAGE, response = ErrorDto.class),
})
public class InstallationAdminController implements ErrorHandlingController {

    private final InstallationFacade installationFacade;

    public InstallationAdminController(InstallationFacade installationFacade) {
        this.installationFacade = installationFacade;
    }

    @GetMapping
    @ApiOperation("Get installation details.")
    @ApiResponses({
            @ApiResponse(code = SC_NOT_FOUND, message = "Not installed.", response = ErrorDto.class),
    })
    public InstallationDto getInstallation() throws ObjectNotFoundException {
        return installationFacade.getInstallation();
    }
}
