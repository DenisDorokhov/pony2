package net.dorokhov.pony2.core.installation.service;

import net.dorokhov.pony2.api.installation.domain.Installation;
import net.dorokhov.pony2.api.installation.service.InstallationService;
import net.dorokhov.pony2.api.installation.service.exception.NotInstalledException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstallationUpgradeRunnerTest {
    
    @InjectMocks
    private InstallationUpgradeRunner installationUpgradeRunner;
    
    @Mock
    private InstallationService installationService;

    @Test
    public void shouldUpgradeIfInstalled() throws NotInstalledException {

        when(installationService.getInstallation()).thenReturn(Optional.of(new Installation()));

        installationUpgradeRunner.run(new DefaultApplicationArguments());

        verify(installationService).getInstallation();
        verify(installationService).upgradeIfNeeded();
    }

    @Test
    public void shouldNotUpgradeWhenNotInstalled() throws NotInstalledException {

        when(installationService.getInstallation()).thenReturn(Optional.empty());

        installationUpgradeRunner.run(new DefaultApplicationArguments());

        verify(installationService).getInstallation();
        verify(installationService, never()).upgradeIfNeeded();
    }
}
