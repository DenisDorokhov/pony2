package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.web.dto.InstallationDto;
import net.dorokhov.pony2.web.service.InstallationFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class InstallationAdminController implements ErrorHandlingController {

    private final InstallationFacade installationFacade;

    public InstallationAdminController(InstallationFacade installationFacade) {
        this.installationFacade = installationFacade;
    }

    @GetMapping("/api/admin/installation")
    public InstallationDto getInstallation() throws ObjectNotFoundException {
        return installationFacade.getInstallation();
    }
}
