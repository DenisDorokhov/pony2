package net.dorokhov.pony.web.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import net.dorokhov.pony.web.security.token.exception.InvalidTokenException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TokenManagerTest {

    @InjectMocks
    private TokenManager tokenManager;

    @Mock
    private TokenSecretManager tokenSecretManager;

    @Test
    public void shouldNotGenerateSecretIfItExists() throws IOException, SecretNotFoundException {

        tokenManager.assureTokenSecretExists();

        verify(tokenSecretManager).fetchTokenSecret();
        verify(tokenSecretManager, never()).generateAndStoreTokenSecret();
    }

    @Test
    public void shouldGenerateSecretIfItDoesNotExist() throws SecretNotFoundException, IOException {

        when(tokenSecretManager.fetchTokenSecret()).thenThrow(new SecretNotFoundException());

        tokenManager.assureTokenSecretExists();

        verify(tokenSecretManager).fetchTokenSecret();
        verify(tokenSecretManager).generateAndStoreTokenSecret();
    }

    @Test
    public void shouldVerifyCreatedAccessToken() throws SecretNotFoundException, IOException, InvalidTokenException {

        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");

        String token = tokenManager.createAccessTokenForUserId("1");

        assertThat(token).isNotNull();
        assertThat(tokenManager.verifyAccessTokenAndGetUserId(token)).isEqualTo("1");
    }

    @Test
    public void shouldFailAccessTokenVerificationOnNotFoundSecret() throws SecretNotFoundException, IOException {

        when(tokenSecretManager.fetchTokenSecret()).thenThrow(new SecretNotFoundException());

        //noinspection ResultOfMethodCallIgnored
        assertThatThrownBy(() -> tokenManager.verifyAccessTokenAndGetUserId("someToken"));
    }

    @Test
    public void shouldFailAccessTokenVerificationOnInvalidToken() throws SecretNotFoundException, IOException {

        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");

        assertThatThrownBy(() -> tokenManager.verifyAccessTokenAndGetUserId("invalidToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailAccessTokenVerificationOnNoSubject() throws SecretNotFoundException, IOException {

        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");
        String token = JWT.create().sign(Algorithm.HMAC256("someSecret"));

        assertThatThrownBy(() -> tokenManager.verifyAccessTokenAndGetUserId(token)).isInstanceOf(InvalidTokenException.class);
    }
}