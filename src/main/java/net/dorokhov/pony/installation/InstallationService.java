package net.dorokhov.pony.installation;

import net.dorokhov.pony.entity.Installation;
import net.dorokhov.pony.installation.exception.AlreadyInstalledException;
import net.dorokhov.pony.installation.exception.NotInstalledException;

public interface InstallationService {

    Installation getInstallation();

    Installation install(InstallationDraft draft) throws AlreadyInstalledException;

    void uninstall() throws NotInstalledException;
}
