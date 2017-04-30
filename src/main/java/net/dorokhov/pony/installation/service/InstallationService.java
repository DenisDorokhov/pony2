package net.dorokhov.pony.installation.service;

import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.service.command.InstallationDraft;
import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.installation.service.exception.NotInstalledException;

import java.util.Optional;

public interface InstallationService {

    Optional<Installation> getInstallation();

    Installation install(InstallationDraft draft) throws AlreadyInstalledException;
    
    Optional<Installation> upgradeIfNeeded() throws NotInstalledException;
}
