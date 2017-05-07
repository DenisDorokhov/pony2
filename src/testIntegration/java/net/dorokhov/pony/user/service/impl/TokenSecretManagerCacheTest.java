package net.dorokhov.pony.user.service.impl;

import net.dorokhov.pony.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenSecretManagerCacheTest extends IntegrationTest {
    
    @Autowired
    private TokenSecretManager tokenSecretManager;

    @Test
    public void shouldCacheAfterFetching() throws Exception {
        String token = tokenSecretManager.getTokenSecret();
        String cachedToken = tokenSecretManager.getTokenSecret();
        assertThat(token).isSameAs(cachedToken);
    }

    @Test
    public void shouldCacheAfterGeneration() throws Exception {
        String token = tokenSecretManager.generateAndStoreTokenSecret();
        String cachedToken = tokenSecretManager.getTokenSecret();
        assertThat(token).isSameAs(cachedToken);
    }
}
