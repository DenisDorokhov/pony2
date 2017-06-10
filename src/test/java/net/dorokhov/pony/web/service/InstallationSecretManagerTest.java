package net.dorokhov.pony.web.service;

import net.dorokhov.pony.common.SecretManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    public void shouldGenerateAndStoreTokenSecret() throws Exception {
        when(secretManager.generateAndStoreSecret(any())).thenReturn("someSecret");
        assertThat(installationSecretManager.generateAndStoreInstallationSecret()).isEqualTo("someSecret");
    }

    @Test
    public void shouldFetchTokenSecret() throws Exception {
        when(secretManager.fetchSecret(any())).thenReturn("someSecret");
        assertThat(installationSecretManager.fetchInstallationSecret()).isEqualTo("someSecret");
    }
}