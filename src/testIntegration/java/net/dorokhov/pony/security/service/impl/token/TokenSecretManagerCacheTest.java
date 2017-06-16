package net.dorokhov.pony.security.service.impl.token;

import net.dorokhov.pony.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenSecretManagerCacheTest extends IntegrationTest {
    
    @Autowired
    private TokenSecretManager tokenSecretManager;

    @Test
    public void shouldCacheAfterFetching() throws Exception {
        String token = tokenSecretManager.fetchTokenSecret();
        String cachedToken = tokenSecretManager.fetchTokenSecret();
        assertThat(token).isSameAs(cachedToken);
    }

    @Test
    public void shouldCacheAfterGeneration() throws Exception {
        String token = tokenSecretManager.generateAndStoreTokenSecret();
        String cachedToken = tokenSecretManager.fetchTokenSecret();
        assertThat(token).isSameAs(cachedToken);
    }
}
