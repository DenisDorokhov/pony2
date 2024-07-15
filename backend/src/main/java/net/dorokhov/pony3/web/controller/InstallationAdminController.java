package net.dorokhov.pony3.web.controller;

import net.dorokhov.pony3.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony3.web.dto.InstallationDto;
import net.dorokhov.pony3.web.service.InstallationFacade;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/admin/installation", produces = APPLICATION_JSON_VALUE)
public class InstallationAdminController implements ErrorHandlingController {

    private final InstallationFacade installationFacade;

    public InstallationAdminController(InstallationFacade installationFacade) {
        this.installationFacade = installationFacade;
    }

    @GetMapping
    public InstallationDto getInstallation() throws ObjectNotFoundException {
        return installationFacade.getInstallation();
    }
}
