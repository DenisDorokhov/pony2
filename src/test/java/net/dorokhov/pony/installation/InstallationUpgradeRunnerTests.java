package net.dorokhov.pony.installation;

import net.dorokhov.pony.entity.Installation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InstallationUpgradeRunnerTests {
    
    @InjectMocks
    private InstallationUpgradeRunner installationUpgradeRunner;
    
    @Mock
    private InstallationService installationService;

    @Test
    public void upgradeIfInstalled() throws Exception {
        given(installationService.getInstallation()).willReturn(Optional.of(Installation.builder()
                .version("2.0")
                .build()));
        installationUpgradeRunner.run(new DefaultApplicationArguments(new String[]{}));
        verify(installationService).getInstallation();
        verify(installationService).upgradeIfNeeded();
    }

    @Test
    public void doNotUpgradeWhenNotInstalled() throws Exception {
        given(installationService.getInstallation()).willReturn(Optional.empty());
        installationUpgradeRunner.run(new DefaultApplicationArguments(new String[]{}));
        verify(installationService).getInstallation();
        verify(installationService, never()).upgradeIfNeeded();
    }
}
