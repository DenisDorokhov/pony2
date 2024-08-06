package net.dorokhov.pony2.api.installation.service;

import net.dorokhov.pony2.api.installation.domain.Installation;
import net.dorokhov.pony2.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony2.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony2.api.installation.service.exception.NotInstalledException;

import java.util.Optional;

public interface InstallationService {

    Optional<Installation> getInstallation();

    Installation install(InstallationCommand command) throws AlreadyInstalledException;
    
    Installation upgradeIfNeeded() throws NotInstalledException;
}
