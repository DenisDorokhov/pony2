package net.dorokhov.pony.core.installation.service;

import net.dorokhov.pony.api.installation.service.InstallationService;
import net.dorokhov.pony.api.installation.service.exception.NotInstalledException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.DefaultApplicationArguments;

import static net.dorokhov.pony.test.InstallationFixtures.installation;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InstallationUpgradeRunnerTest {
    
    @InjectMocks
    private InstallationUpgradeRunner installationUpgradeRunner;
    
    @Mock
    private InstallationService installationService;

    @Test
    public void shouldUpgradeIfInstalled() throws NotInstalledException {

        when(installationService.getInstallation()).thenReturn(installation());

        installationUpgradeRunner.run(new DefaultApplicationArguments(new String[]{}));

        verify(installationService).getInstallation();
        verify(installationService).upgradeIfNeeded();
    }

    @Test
    public void shouldNotUpgradeWhenNotInstalled() throws NotInstalledException {

        when(installationService.getInstallation()).thenReturn(null);

        installationUpgradeRunner.run(new DefaultApplicationArguments(new String[]{}));

        verify(installationService).getInstallation();
        verify(installationService, never()).upgradeIfNeeded();
    }
}
