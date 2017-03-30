package net.dorokhov.pony.installation;

import net.dorokhov.pony.entity.Installation;

public interface InstallationService {

    Installation getInstallation();

    Installation install(InstallCommand command) throws AlreadyInstalledException;

    void uninstall() throws NotInstalledException;
}
