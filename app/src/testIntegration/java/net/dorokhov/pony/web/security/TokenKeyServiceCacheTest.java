package net.dorokhov.pony.web.security;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.web.security.token.TokenKeyService;
import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenKeyServiceCacheTest extends IntegrationTest {
    
    @Autowired
    private TokenKeyService tokenKeyService;

    @Test
    public void shouldCacheAccessTokenKeyAfterFetching() throws SecretNotFoundException, IOException {

        byte[] token = tokenKeyService.fetchAccessTokenKey();
        byte[] cachedToken = tokenKeyService.fetchAccessTokenKey();

        assertThat(token).isSameAs(cachedToken);
    }

    @Test
    public void shouldCacheAccessTokenKeyAfterGeneration() throws SecretNotFoundException, IOException {

        byte[] token = tokenKeyService.generateAndStoreAccessTokenKey();
        byte[] cachedToken = tokenKeyService.fetchAccessTokenKey();

        assertThat(token).isSameAs(cachedToken);
    }

    @Test
    public void shouldCacheStaticTokenKeyAfterFetching() throws SecretNotFoundException, IOException {

        byte[] token = tokenKeyService.fetchStaticTokenKey();
        byte[] cachedToken = tokenKeyService.fetchStaticTokenKey();

        assertThat(token).isSameAs(cachedToken);
    }

    @Test
    public void shouldCacheStaticTokenKeyAfterGeneration() throws SecretNotFoundException, IOException {

        byte[] token = tokenKeyService.generateAndStoreStaticTokenKey();
        byte[] cachedToken = tokenKeyService.fetchStaticTokenKey();

        assertThat(token).isSameAs(cachedToken);
    }
}
