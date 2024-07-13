package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.api.installation.domain.Installation;
import net.dorokhov.pony3.api.installation.service.InstallationService;
import net.dorokhov.pony3.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony3.web.dto.InstallationCommandDto;
import net.dorokhov.pony3.web.service.exception.InvalidInstallationSecretException;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import net.dorokhov.pony3.web.service.exception.SecretNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstallationFacadeImplTest {

    @InjectMocks
    private InstallationFacade installationServiceFacade;

    @Mock
    private InstallationService installationService;
    @Mock
    private InstallationSecretService installationSecretService;

    @Test
    public void shouldGetInstallation() throws ObjectNotFoundException {

        Installation installation = new Installation()
                .setCreationDate(LocalDateTime.now())
                .setVersion("2.0");
        when(installationService.getInstallation()).thenReturn(Optional.of(installation));

        assertThat(installationServiceFacade.getInstallation()).satisfies(installationDto ->
                assertThat(installationDto.getVersion()).isEqualTo("2.0"));
    }

    @Test
    public void shouldGetNoInstallation() {

        when(installationService.getInstallation()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> installationServiceFacade.getInstallation()).isInstanceOfSatisfying(ObjectNotFoundException.class, e -> {
            assertThat(e.getObjectType()).isEqualTo(Installation.class);
            assertThat(e.getObjectId()).isNull();
        });
    }

    @Test
    public void shouldVerifyInstallationSecret() throws SecretNotFoundException, IOException {

        when(installationSecretService.fetchInstallationSecret()).thenReturn("someSecret");

        assertThat(installationServiceFacade.verifyInstallationSecret("someSecret")).isTrue();
    }

    @Test
    public void shouldNotVerifyInstallationSecretIfItCannotBeFound() throws SecretNotFoundException, IOException {

        when(installationSecretService.fetchInstallationSecret()).thenThrow(new SecretNotFoundException());

        assertThat(installationServiceFacade.verifyInstallationSecret("someSecret")).isFalse();
    }

    @Test
    public void shouldNotVerifyInstallationSecretIfItCannotBeRead() throws SecretNotFoundException, IOException {

        when(installationSecretService.fetchInstallationSecret()).thenThrow(new IOException());

        assertThat(installationServiceFacade.verifyInstallationSecret("someSecret")).isFalse();
    }

    @Test
    public void shouldInstall() throws SecretNotFoundException, IOException, AlreadyInstalledException, InvalidInstallationSecretException {

        when(installationSecretService.fetchInstallationSecret()).thenReturn("someSecret");
        Installation installation = new Installation()
                .setCreationDate(LocalDateTime.now())
                .setVersion("2.0");
        when(installationService.install(any())).thenReturn(installation);
        InstallationCommandDto command = new InstallationCommandDto()
                .setInstallationSecret("someSecret")
                .setLibraryFolders(emptyList())
                .setAdminName("Foo Bar")
                .setAdminEmail("foo@bar.com")
                .setAdminPassword("somePassword")
                .setRepeatAdminPassword("somePassword");

        assertThat(installationServiceFacade.install(command)).satisfies(installationDto ->
                assertThat(installationDto.getVersion()).isEqualTo("2.0"));
    }

    @Test
    public void shouldFailInstallationIfSecretIsInvalid() throws SecretNotFoundException, IOException {

        when(installationSecretService.fetchInstallationSecret()).thenReturn("someSecret");
        InstallationCommandDto command = new InstallationCommandDto()
                .setInstallationSecret("invalidSecret")
                .setLibraryFolders(emptyList())
                .setAdminName("Foo Bar")
                .setAdminEmail("foo@bar.com")
                .setAdminPassword("somePassword")
                .setRepeatAdminPassword("somePassword");

        assertThatThrownBy(() -> installationServiceFacade.install(command)).isInstanceOf(InvalidInstallationSecretException.class);
    }

    @Test
    public void shouldGenerateSecretIfItDoesNotNotExists() throws SecretNotFoundException, IOException {

        when(installationSecretService.fetchInstallationSecret()).thenThrow(new SecretNotFoundException());

        installationServiceFacade.assureInstallationSecretExists();

        verify(installationSecretService).generateAndStoreInstallationSecret();
    }

    @Test
    public void shouldNotGenerateSecretIfItExists() throws SecretNotFoundException, IOException {

        when(installationSecretService.fetchInstallationSecret()).thenReturn("someSecret");

        installationServiceFacade.assureInstallationSecretExists();

        verify(installationSecretService, never()).generateAndStoreInstallationSecret();
    }
}