package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.service.InstallationServiceFacade;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/installation")
public class InstallationController implements ResponseBodyController {
    
    private final InstallationServiceFacade installationServiceFacade;

    public InstallationController(InstallationServiceFacade installationServiceFacade) {
        this.installationServiceFacade = installationServiceFacade;
    }

    @PostMapping
    public InstallationDto install(@Valid @RequestBody InstallationCommandDto command) throws AlreadyInstalledException {
        return installationServiceFacade.install(command);
    }
}
