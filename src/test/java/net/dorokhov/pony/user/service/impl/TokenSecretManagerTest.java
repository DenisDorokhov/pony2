package net.dorokhov.pony.user.service.impl;

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
public class TokenSecretManagerTest {
    
    @InjectMocks
    private TokenSecretManager tokenSecretManager;
    
    @Mock
    private SecretManager secretManager;

    @Test
    public void shouldGenerateAndStoreTokenSecret() throws Exception {
        when(secretManager.generateAndStoreSecret(any())).thenReturn("someSecret");
        assertThat(tokenSecretManager.generateAndStoreTokenSecret()).isEqualTo("someSecret");
    }

    @Test
    public void shouldFetchTokenSecret() throws Exception {
        when(secretManager.fetchSecret(any())).thenReturn("someSecret");
        assertThat(tokenSecretManager.fetchTokenSecret()).isEqualTo("someSecret");
    }
}
