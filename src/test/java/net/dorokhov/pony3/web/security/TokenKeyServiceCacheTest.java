package net.dorokhov.pony3.web.security;

import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.web.security.token.TokenKeyService;
import net.dorokhov.pony3.web.service.exception.SecretNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenKeyServiceCacheTest extends IntegrationTest {
    
    @Autowired
    private TokenKeyService tokenKeyService;

    @Test
    public void shouldCacheAccessTokenKeyAfterFetching() throws SecretNotFoundException {

        byte[] token = tokenKeyService.fetchAccessTokenKey();
        byte[] cachedToken = tokenKeyService.fetchAccessTokenKey();

        assertThat(token).isSameAs(cachedToken);
    }

    @Test
    public void shouldCacheAccessTokenKeyAfterGeneration() throws SecretNotFoundException {

        byte[] token = tokenKeyService.generateAndStoreAccessTokenKey();
        byte[] cachedToken = tokenKeyService.fetchAccessTokenKey();

        assertThat(token).isSameAs(cachedToken);
    }

    @Test
    public void shouldCacheStaticTokenKeyAfterFetching() throws SecretNotFoundException {

        byte[] token = tokenKeyService.fetchStaticTokenKey();
        byte[] cachedToken = tokenKeyService.fetchStaticTokenKey();

        assertThat(token).isSameAs(cachedToken);
    }

    @Test
    public void shouldCacheStaticTokenKeyAfterGeneration() throws SecretNotFoundException {

        byte[] token = tokenKeyService.generateAndStoreStaticTokenKey();
        byte[] cachedToken = tokenKeyService.fetchStaticTokenKey();

        assertThat(token).isSameAs(cachedToken);
    }
}
