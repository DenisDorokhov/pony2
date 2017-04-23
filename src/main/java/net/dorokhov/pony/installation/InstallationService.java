package net.dorokhov.pony.installation;

import net.dorokhov.pony.entity.Installation;
import net.dorokhov.pony.installation.domain.InstallationDraft;
import net.dorokhov.pony.installation.exception.AlreadyInstalledException;
import net.dorokhov.pony.installation.exception.NotInstalledException;

import java.util.Optional;

public interface InstallationService {

    Optional<Installation> getInstallation();

    Installation install(InstallationDraft draft) throws AlreadyInstalledException;
    
    Installation upgrade() throws NotInstalledException;
}
