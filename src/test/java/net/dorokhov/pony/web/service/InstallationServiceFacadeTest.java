package net.dorokhov.pony.web.service;

import net.dorokhov.pony.common.SecretNotFoundException;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstallationServiceFacadeTest {
    
    @InjectMocks
    private InstallationServiceFacade installationServiceFacade;

    @Mock
    private InstallationService installationService;
    @Mock
    private InstallationSecretManager installationSecretManager;

    @Test
    public void shouldGetInstallation() throws Exception {
        Installation installation = Installation.builder()
                .version("2.0")
                .build();
        when(installationService.getInstallation()).thenReturn(installation);
        assertThat(installationServiceFacade.getInstallation()).satisfies(installationDto -> 
                assertThat(installationDto.getVersion()).isEqualTo("2.0"));
    }

    @Test
    public void shouldGetNoInstallation() throws Exception {
        when(installationService.getInstallation()).thenReturn(null);
        assertThat(installationServiceFacade.getInstallation()).isNull();
    }

    @Test
    public void shouldInstall() throws Exception {
        Installation installation = Installation.builder()
                .version("2.0")
                .build();
        when(installationService.install(any())).thenReturn(installation);
        InstallationCommandDto command = new InstallationCommandDto("someSecret", emptyList(), 
                "Foo Bar", "foo@bar.com", "somePassword");
        assertThat(installationServiceFacade.install(command)).satisfies(installationDto -> 
                assertThat(installationDto.getVersion()).isEqualTo("2.0"));
    }

    @Test
    public void shouldGenerateSecretIfItDoesNotNotExists() throws Exception {
        when(installationSecretManager.fetchInstallationSecret()).thenThrow(new SecretNotFoundException());
        installationServiceFacade.assureInstallationSecretExists();
        verify(installationSecretManager).generateAndStoreInstallationSecret();
    }

    @Test
    public void shouldNotGenerateSecretIfItExists() throws Exception {
        when(installationSecretManager.fetchInstallationSecret()).thenReturn("someSecret");
        installationServiceFacade.assureInstallationSecretExists();
        verify(installationSecretManager, never()).generateAndStoreInstallationSecret();
    }
}