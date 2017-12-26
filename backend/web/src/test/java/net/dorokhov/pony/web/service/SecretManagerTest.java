package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SecretManagerTest {

    private static final Resource SECRET_RESOURCE = new ClassPathResource("test.txt");

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void shouldGenerateAndStoreTokenSecret() throws IOException, SecretNotFoundException {

        SecretManager secretManager = new SecretManager();
        File file = tempFolder.newFile();

        assertThat(secretManager.generateAndStoreSecret(file)).isNotNull();
        assertThat(secretManager.fetchSecret(file)).isNotNull();
    }

    @Test
    public void shouldGetTokenSecret() throws IOException, SecretNotFoundException {

        SecretManager secretManager = new SecretManager();

        assertThat(secretManager.fetchSecret(SECRET_RESOURCE.getFile())).isEqualTo("test");
    }

    @Test
    public void shouldFailGettingNotExistingTokenSecret() {

        SecretManager secretManager = new SecretManager();
        File file = new File("notExistingFile");

        assertThatThrownBy(() -> secretManager.fetchSecret(file)).isInstanceOf(SecretNotFoundException.class);
    }
}