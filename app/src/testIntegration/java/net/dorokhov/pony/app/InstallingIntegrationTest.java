package net.dorokhov.pony.app;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.installation.service.InstallationService;
import net.dorokhov.pony.api.installation.service.command.InstallationCommand;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

public abstract class InstallingIntegrationTest extends IntegrationTest {
    
    public static final String ADMIN_NAME = "Foo Bar";
    public static final String ADMIN_EMAIL = "foo@bar.com";
    public static final String ADMIN_PASSWORD = "foobar";
    public static final int AUTO_SCAN_INTERVAL = 60;
    
    @Autowired
    private InstallationService installationService;
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    protected File libraryFolder;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        libraryFolder = tempFolder.getRoot();
        InstallationCommand command = InstallationCommand.builder()
                .adminName(ADMIN_NAME)
                .adminEmail(ADMIN_EMAIL)
                .adminPassword(ADMIN_PASSWORD)
                .libraryFolders(ImmutableList.of(libraryFolder))
                .autoScanInterval(AUTO_SCAN_INTERVAL)
                .build();
        installationService.install(command);
    }
}
