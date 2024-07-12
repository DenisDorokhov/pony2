package net.dorokhov.pony3.api.installation.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony3.api.installation.domain.Installation;
import net.dorokhov.pony3.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony3.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony3.api.installation.service.exception.NotInstalledException;

public interface InstallationService {

    @Nullable
    Installation getInstallation();

    Installation install(InstallationCommand command) throws AlreadyInstalledException;
    
    Installation upgradeIfNeeded() throws NotInstalledException;
}
