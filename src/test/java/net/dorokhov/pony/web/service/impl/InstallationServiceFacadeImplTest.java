package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.common.SecretNotFoundException;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.service.exception.InvalidInstallationSecretException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InstallationServiceFacadeImplTest {
    
    @InjectMocks
    private InstallationServiceFacadeImpl installationServiceFacade;

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
    public void shouldVerifyInstallationSecret() throws Exception {
        when(installationSecretManager.fetchInstallationSecret()).thenReturn("someSecret");
        assertThat(installationServiceFacade.verifyInstallationSecret("someSecret")).isTrue();
    }

    @Test
    public void shouldNotVerifyInstallationSecretIfItCannotBeFound() throws Exception {
        when(installationSecretManager.fetchInstallationSecret()).thenThrow(new SecretNotFoundException());
        assertThat(installationServiceFacade.verifyInstallationSecret("someSecret")).isFalse();
    }

    @Test
    public void shouldNotVerifyInstallationSecretIfItCannotBeRead() throws Exception {
        when(installationSecretManager.fetchInstallationSecret()).thenThrow(new IOException());
        assertThat(installationServiceFacade.verifyInstallationSecret("someSecret")).isFalse();
    }

    @Test
    public void shouldInstall() throws Exception {
        when(installationSecretManager.fetchInstallationSecret()).thenReturn("someSecret");
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
    public void shouldFailInstallationIfSecretIsInvalid() throws Exception {
        when(installationSecretManager.fetchInstallationSecret()).thenReturn("someSecret");
        InstallationCommandDto command = new InstallationCommandDto("invalidSecret", emptyList(),
                "Foo Bar", "foo@bar.com", "somePassword");
        assertThatThrownBy(() -> installationServiceFacade.install(command)).isInstanceOf(InvalidInstallationSecretException.class);
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