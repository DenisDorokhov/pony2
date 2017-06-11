package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.service.InstallationServiceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/installation")
public class InstallationController implements ResponseBodyController {

    @ControllerAdvice(assignableTypes = InstallationController.class)
    @ResponseBody
    public static class Advice {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        @ExceptionHandler(AlreadyInstalledException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onAlreadyInstalled() {
            logger.warn("Application is already installed.");
            return new ErrorDto(ErrorDto.Code.BAD_REQUEST, "Bad request.");
        }
    }
    
    private final InstallationServiceFacade installationServiceFacade;

    public InstallationController(InstallationServiceFacade installationServiceFacade) {
        this.installationServiceFacade = installationServiceFacade;
    }

    @PostMapping
    public InstallationDto install(@Valid @RequestBody InstallationCommandDto command) throws AlreadyInstalledException {
        return installationServiceFacade.install(command);
    }
}
