package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.domain.InstallationStatusDto;
import net.dorokhov.pony.web.service.InstallationFacade;
import net.dorokhov.pony.web.service.exception.InvalidInstallationSecretException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/installation", produces = APPLICATION_JSON_VALUE)
@Api(tags = "Installation")
public class InstallationController implements ErrorHandlingController {

    @ControllerAdvice(assignableTypes = InstallationController.class)
    @ResponseBody
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public static class Advice {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        @ExceptionHandler(AlreadyInstalledException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onAlreadyInstalled() {
            logger.warn("Application is already installed.");
            return ErrorDto.badRequest();
        }
    }

    private final InstallationFacade installationFacade;

    public InstallationController(InstallationFacade installationFacade) {
        this.installationFacade = installationFacade;
    }
    
    @GetMapping("/status")
    @ApiOperation("Get installation status.")
    public InstallationStatusDto getInstallationStatus() {
        return installationFacade.getInstallationStatus();
    }

    @PostMapping
    @ApiOperation("Perform installation.")
    @ApiResponses({
            @ApiResponse(code = SC_BAD_REQUEST, message = "Already installed or invalid request.", response = ErrorDto.class),
    })
    public InstallationDto install(@Valid @RequestBody InstallationCommandDto command) throws InvalidInstallationSecretException, AlreadyInstalledException {
        return installationFacade.install(command);
    }
}
