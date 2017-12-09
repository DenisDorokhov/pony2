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
    public void shouldNotGenerateSecretIfItExists() throws Exception {
        tokenManager.assureTokenSecretExists();
        verify(tokenSecretManager).fetchTokenSecret();
        verify(tokenSecretManager, never()).generateAndStoreTokenSecret();
    }

    @Test
    public void shouldGenerateSecretIfItDoesNotExist() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenThrow(new SecretNotFoundException());
        tokenManager.assureTokenSecretExists();
        verify(tokenSecretManager).fetchTokenSecret();
        verify(tokenSecretManager).generateAndStoreTokenSecret();
    }

    @Test
    public void shouldVerifyCreatedToken() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");
        String token = tokenManager.createToken("1");
        assertThat(token).isNotNull();
        assertThat(tokenManager.verifyToken(token)).isEqualTo(1L);
    }

    @Test
    public void shouldVerifyToken() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");
        String token = JWT.create()
                .withSubject("1")
                .sign(Algorithm.HMAC256("someSecret"));
        assertThat(tokenManager.verifyToken(token)).isEqualTo(1L);
    }

    @Test
    public void shouldFailVerificationOnNotFoundSecret() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenThrow(new SecretNotFoundException());
        assertThatThrownBy(() -> tokenManager.verifyToken("someToken"));
    }

    @Test
    public void shouldFailVerificationOnInvalidToken() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");
        assertThatThrownBy(() -> tokenManager.verifyToken("invalidToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailVerificationOnNoSubject() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");
        String token = JWT.create().sign(Algorithm.HMAC256("someSecret"));
        assertThatThrownBy(() -> tokenManager.verifyToken(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailVerificationOnInvalidSubject() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");
        String token = JWT.create()
                .withSubject("invalidSubject")
                .sign(Algorithm.HMAC256("someSecret"));
        assertThatThrownBy(() -> tokenManager.verifyToken(token)).isInstanceOf(InvalidTokenException.class);
    }
}