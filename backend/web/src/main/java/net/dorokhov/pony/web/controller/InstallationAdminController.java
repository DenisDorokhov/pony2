package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.service.InstallationFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/installation", produces = "application/json")
@Api(tags = "Installation Administration")
public class InstallationAdminController implements ErrorHandlingController {
    
    private final InstallationFacade installationFacade;

    public InstallationAdminController(InstallationFacade installationFacade) {
        this.installationFacade = installationFacade;
    }

    @GetMapping
    @ApiOperation("Get installation details.")
    public InstallationDto getInstallation() throws ObjectNotFoundException {
        return installationFacade.getInstallation();
    }
}
