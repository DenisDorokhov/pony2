package net.dorokhov.pony.installation.service;

import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.service.command.InstallationCommand;
import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.installation.service.exception.NotInstalledException;

import javax.annotation.Nullable;

public interface InstallationService {

    @Nullable
    Installation getInstallation();

    Installation install(InstallationCommand command) throws AlreadyInstalledException;
    
    Installation upgradeIfNeeded() throws NotInstalledException;
}
