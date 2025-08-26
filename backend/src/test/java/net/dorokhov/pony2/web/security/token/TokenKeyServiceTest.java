package net.dorokhov.pony2.web.security.token;

import net.dorokhov.pony2.web.service.RandomKeyService;
import net.dorokhov.pony2.web.service.exception.SecretNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TokenKeyServiceTest {
    
    private TokenKeyService tokenKeyService;

    @Mock
    private RandomKeyService randomKeyService;
    @Mock
    private File accessTokenKeyFile;
    @Mock
    private File staticTokenKeyFile;
    @Mock
    private File openSubsonicKeyFile;

    @BeforeEach
    public void setUp() {
        tokenKeyService = new TokenKeyService(randomKeyService, accessTokenKeyFile, staticTokenKeyFile, openSubsonicKeyFile);
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

    @Test
    public void shouldGenerateAndStoreOpenSubsonicKey() throws IOException {

        byte[] randomKey = new byte[]{1, 2, 3};

        when(randomKeyService.generateAndStoreRandomKey(openSubsonicKeyFile)).thenReturn(randomKey);

        assertThat(tokenKeyService.generateAndStoreOpenSubsonicKey()).isEqualTo(randomKey);
    }

    @Test
    public void shouldFetchOpenSubsonicKey() throws SecretNotFoundException, IOException {

        byte[] randomKey = new byte[]{1, 2, 3};

        when(randomKeyService.fetchStoredKey(openSubsonicKeyFile)).thenReturn(randomKey);

        assertThat(tokenKeyService.fetchOpenSubsonicKey()).isEqualTo(randomKey);
    }
}
