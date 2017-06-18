package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.service.InstallationFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/installation")
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
