package net.dorokhov.pony.web.controller;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.dorokhov.pony.api.installation.service.exception.AlreadyInstalledException;
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
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/installation", produces = "application/json")
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
    public InstallationDto install(@Valid @RequestBody InstallationCommandDto command) throws InvalidInstallationSecretException, AlreadyInstalledException {
        return installationFacade.install(command);
    }
}
