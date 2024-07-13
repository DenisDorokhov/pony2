package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.web.service.exception.SecretNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InstallationSecretServiceTest {

    private static final Resource SECRET_RESOURCE = new ClassPathResource("test.txt");
    
    @Mock
    private RandomKeyService randomKeyService;

    @TempDir
    public Path tempFolder;

    @Test
    public void shouldGenerateAndStoreInstallationSecret() throws IOException, SecretNotFoundException {

        InstallationSecretService installationSecretService = new InstallationSecretService(randomKeyService, Files.createFile(tempFolder.resolve("file")).toFile());
        
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