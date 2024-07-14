package net.dorokhov.pony3;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony3.api.installation.service.InstallationService;
import net.dorokhov.pony3.api.installation.service.command.InstallationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.file.Path;

public abstract class InstallingIntegrationTest extends IntegrationTest {
    
    public static final String ADMIN_NAME = "Foo Bar";
    public static final String ADMIN_EMAIL = "foo@bar.com";
    public static final String ADMIN_PASSWORD = "foobar";
    public static final int AUTO_SCAN_INTERVAL = 60;
    
    @Autowired
    private InstallationService installationService;
    
    @TempDir
    public Path tempFolder;
    
    protected File libraryFolder;

    @BeforeEach
    public void setUpInstallingIntegrationTest() throws Exception {
        libraryFolder = tempFolder.toFile();
        InstallationCommand command = new InstallationCommand()
                .setAdminName(ADMIN_NAME)
                .setAdminEmail(ADMIN_EMAIL)
                .setAdminPassword(ADMIN_PASSWORD)
                .setLibraryFolders(ImmutableList.of(libraryFolder))
                .setAutoScanInterval(AUTO_SCAN_INTERVAL);
        installationService.install(command);
    }
}
