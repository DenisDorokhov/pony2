package net.dorokhov.pony;

import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.installation.service.command.InstallationCommand;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class InstallingIntegrationTest extends IntegrationTest {
    
    public static final String ADMIN_NAME = "Foo Bar";
    public static final String ADMIN_EMAIL = "foo@bar.com";
    public static final String ADMIN_PASSWORD = "foobar";
    
    @Autowired
    private InstallationService installationService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        InstallationCommand command = InstallationCommand.builder()
                .adminName(ADMIN_NAME)
                .adminEmail(ADMIN_EMAIL)
                .adminPassword(ADMIN_PASSWORD)
                .build();
        installationService.install(command);
    }
}
