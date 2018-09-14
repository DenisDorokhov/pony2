package net.dorokhov.pony.web.security.token;

import net.dorokhov.pony.web.service.RandomKeyService;
import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TokenKeyServiceTest {
    
    private TokenKeyService tokenKeyService;

    @Mock
    private RandomKeyService randomKeyService;
    @Mock
    private File accessTokenKeyFile;
    @Mock
    private File staticTokenKeyFile;

    @Before
    public void setUp() {
        tokenKeyService = new TokenKeyService(randomKeyService, accessTokenKeyFile, staticTokenKeyFile);
    }

    @Test
    public void shouldGenerateAndStoreAccessTokenKey() throws IOException {
        
        byte[] randomKey = new byte[]{1, 2, 3};

        when(randomKeyService.generateAndStoreRandomKey(accessTokenKeyFile)).thenReturn(randomKey);

        assertThat(tokenKeyService.generateAndStoreAccessTokenKey()).isEqualTo(randomKey);
    }

    @Test
    public void shouldFetchAccessTokenKey() throws SecretNotFoundException, IOException {

        byte[] randomKey = new byte[]{1, 2, 3};

        when(randomKeyService.fetchStoredKey(accessTokenKeyFile)).thenReturn(randomKey);

        assertThat(tokenKeyService.fetchAccessTokenKey()).isEqualTo(randomKey);
    }

    @Test
    public void shouldGenerateAndStoreStaticTokenKey() throws IOException {
        
        byte[] randomKey = new byte[]{1, 2, 3};

        when(randomKeyService.generateAndStoreRandomKey(staticTokenKeyFile)).thenReturn(randomKey);

        assertThat(tokenKeyService.generateAndStoreStaticTokenKey()).isEqualTo(randomKey);
    }

    @Test
    public void shouldFetchStaticTokenKey() throws SecretNotFoundException, IOException {

        byte[] randomKey = new byte[]{1, 2, 3};

        when(randomKeyService.fetchStoredKey(staticTokenKeyFile)).thenReturn(randomKey);

        assertThat(tokenKeyService.fetchStaticTokenKey()).isEqualTo(randomKey);
    }
}
