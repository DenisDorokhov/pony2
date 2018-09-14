package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstallationSecretServiceTest {

    private static final Resource SECRET_RESOURCE = new ClassPathResource("test.txt");
    
    @Mock
    private RandomKeyService randomKeyService;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void shouldGenerateAndStoreInstallationSecret() throws IOException, SecretNotFoundException {

        InstallationSecretService installationSecretService = new InstallationSecretService(randomKeyService, tempFolder.newFile());
        
        when(randomKeyService.generateRandomKey()).thenReturn(new byte[]{1, 2, 3});

        assertThat(installationSecretService.generateAndStoreInstallationSecret()).isEqualTo("AQID");
        assertThat(installationSecretService.fetchInstallationSecret()).isEqualTo("AQID");
    }

    @Test
    public void shouldFetchInstallationSecret() throws IOException, SecretNotFoundException {

        InstallationSecretService installationSecretService = new InstallationSecretService(randomKeyService, SECRET_RESOURCE.getFile());

        assertThat(installationSecretService.fetchInstallationSecret()).isEqualTo("test");
    }

    @Test
    public void shouldFailFetchingInstallationSecretIfFileDoesNotExist() {

        InstallationSecretService installationSecretService = new InstallationSecretService(randomKeyService, new File("notExistingFile"));

        assertThatThrownBy(installationSecretService::fetchInstallationSecret)
                .isInstanceOf(SecretNotFoundException.class);
    }
}