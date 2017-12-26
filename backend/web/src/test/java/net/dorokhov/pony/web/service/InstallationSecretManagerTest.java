package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstallationSecretManagerTest {

    @InjectMocks
    private InstallationSecretManager installationSecretManager;

    @Mock
    private SecretManager secretManager;

    @Test
    public void shouldGenerateAndStoreTokenSecret() throws IOException {

        when(secretManager.generateAndStoreSecret(any())).thenReturn("someSecret");

        assertThat(installationSecretManager.generateAndStoreInstallationSecret()).isEqualTo("someSecret");
    }

    @Test
    public void shouldFetchTokenSecret() throws SecretNotFoundException, IOException {

        when(secretManager.fetchSecret(any())).thenReturn("someSecret");

        assertThat(installationSecretManager.fetchInstallationSecret()).isEqualTo("someSecret");
    }
}