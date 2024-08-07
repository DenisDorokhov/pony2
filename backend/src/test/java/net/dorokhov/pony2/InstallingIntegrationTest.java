package net.dorokhov.pony2;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.installation.service.InstallationService;
import net.dorokhov.pony2.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony2.web.security.BruteForceProtector;
import net.dorokhov.pony2.web.security.token.TokenKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.file.Path;

public abstract class InstallingIntegrationTest extends IntegrationTest {
    
    public static final String ADMIN_NAME = "Foo Bar";
    public static final String ADMIN_EMAIL = "foo@bar.com";
    public static final String ADMIN_PASSWORD = "foobar";

    @Autowired
    private BruteForceProtector bruteForceProtector;

    @Autowired
    private TokenKeyService tokenKeyService;

    @Autowired
    private InstallationService installationService;

    @TempDir
    public Path tempFolder;
    
    protected File libraryFolder;

    @BeforeEach
    public void setUpInstallingIntegrationTest() throws Exception {
        libraryFolder = tempFolder.toFile();
        bruteForceProtector.clearLoginAttempts();
        tokenKeyService.clearCache();
        InstallationCommand command = new InstallationCommand()
                .setAdminName(ADMIN_NAME)
                .setAdminEmail(ADMIN_EMAIL)
                .setAdminPassword(ADMIN_PASSWORD)
                .setLibraryFolders(ImmutableList.of(libraryFolder));
        installationService.install(command);
    }
}
