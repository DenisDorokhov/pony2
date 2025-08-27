package net.dorokhov.pony2.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony2.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.dto.InstallationCommandDto;
import net.dorokhov.pony2.web.dto.InstallationDto;
import net.dorokhov.pony2.web.dto.InstallationStatusDto;
import net.dorokhov.pony2.web.service.InstallationFacade;
import net.dorokhov.pony2.web.service.exception.InvalidInstallationSecretException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
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
    
    @GetMapping("/api/installation/status")
    public InstallationStatusDto getInstallationStatus() {
        return installationFacade.getInstallationStatus();
    }

    @PostMapping("/api/installation")
    public InstallationDto install(@Valid @RequestBody InstallationCommandDto command) throws InvalidInstallationSecretException, AlreadyInstalledException {
        return installationFacade.install(command);
    }
}
