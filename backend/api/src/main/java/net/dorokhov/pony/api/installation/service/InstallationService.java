package net.dorokhov.pony.api.installation.service;

import net.dorokhov.pony.api.installation.domain.Installation;
import net.dorokhov.pony.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.api.installation.service.exception.NotInstalledException;

import javax.annotation.Nullable;

public interface InstallationService {

    @Nullable
    Installation getInstallation();

    Installation install(InstallationCommand command) throws AlreadyInstalledException;
    
    Installation upgradeIfNeeded() throws NotInstalledException;
}
