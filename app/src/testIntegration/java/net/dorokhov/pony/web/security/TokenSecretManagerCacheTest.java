package net.dorokhov.pony.web.security;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.web.security.token.TokenSecretManager;
import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenSecretManagerCacheTest extends IntegrationTest {
    
    @Autowired
    private TokenSecretManager tokenSecretManager;

    @Test
    public void shouldCacheAfterFetching() throws SecretNotFoundException, IOException {

        String token = tokenSecretManager.fetchTokenSecret();
        String cachedToken = tokenSecretManager.fetchTokenSecret();

        assertThat(token).isSameAs(cachedToken);
    }

    @Test
    public void shouldCacheAfterGeneration() throws SecretNotFoundException, IOException {

        String token = tokenSecretManager.generateAndStoreTokenSecret();
        String cachedToken = tokenSecretManager.fetchTokenSecret();

        assertThat(token).isSameAs(cachedToken);
    }
}
