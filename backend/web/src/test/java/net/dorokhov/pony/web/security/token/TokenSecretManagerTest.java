package net.dorokhov.pony.web.security.token;

import net.dorokhov.pony.web.service.SecretManager;
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
public class TokenSecretManagerTest {

    @InjectMocks
    private TokenSecretManager tokenSecretManager;

    @Mock
    private SecretManager secretManager;

    @Test
    public void shouldGenerateAndStoreTokenSecret() throws IOException {

        when(secretManager.generateAndStoreSecret(any())).thenReturn("someSecret");

        assertThat(tokenSecretManager.generateAndStoreTokenSecret()).isEqualTo("someSecret");
    }

    @Test
    public void shouldFetchTokenSecret() throws SecretNotFoundException, IOException {

        when(secretManager.fetchSecret(any())).thenReturn("someSecret");

        assertThat(tokenSecretManager.fetchTokenSecret()).isEqualTo("someSecret");
    }
}
