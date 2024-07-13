package net.dorokhov.pony3.web.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import net.dorokhov.pony3.web.security.token.exception.InvalidTokenException;
import net.dorokhov.pony3.web.service.exception.SecretNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private TokenKeyService tokenKeyService;

    @Test
    public void shouldNotGenerateKeysIfTheyExist() throws IOException, SecretNotFoundException {
        
        when(tokenKeyService.fetchAccessTokenKey()).thenReturn(new byte[]{});
        when(tokenKeyService.fetchStaticTokenKey()).thenReturn(new byte[]{});

        tokenService.assureTokenKeysExist();

        verify(tokenKeyService, never()).generateAndStoreAccessTokenKey();
        verify(tokenKeyService, never()).generateAndStoreStaticTokenKey();
    }

    @Test
    public void shouldGenerateKeysIdTheyDoNotExist() throws SecretNotFoundException, IOException {

        when(tokenKeyService.fetchAccessTokenKey()).thenThrow(new SecretNotFoundException());
        when(tokenKeyService.fetchStaticTokenKey()).thenThrow(new SecretNotFoundException());

        tokenService.assureTokenKeysExist();

        verify(tokenKeyService).generateAndStoreAccessTokenKey();
        verify(tokenKeyService).generateAndStoreStaticTokenKey();
    }

    @Test
    public void shouldVerifyAccessToken() throws SecretNotFoundException, IOException, InvalidTokenException {

        when(tokenKeyService.fetchAccessTokenKey()).thenReturn(new byte[]{1, 2, 3});

        String token = tokenService.generateAccessTokenForUserId("1");

        assertThat(token).isNotNull();
        assertThat(tokenService.verifyAccessTokenAndGetUserId(token)).isEqualTo("1");
    }

    @Test
    public void shouldFailAccessTokenVerificationIfKeyIsNotFound() throws SecretNotFoundException, IOException {

        when(tokenKeyService.fetchAccessTokenKey()).thenThrow(new SecretNotFoundException());

        //noinspection ResultOfMethodCallIgnored
        assertThatThrownBy(() -> tokenService.verifyAccessTokenAndGetUserId("someToken"));
    }

    @Test
    public void shouldFailAccessTokenVerificationIfTokenIsInvalid() throws SecretNotFoundException, IOException {

        when(tokenKeyService.fetchAccessTokenKey()).thenReturn(new byte[]{});

        assertThatThrownBy(() -> tokenService.verifyAccessTokenAndGetUserId("invalidToken"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailAccessTokenVerificationOnNoSubject() throws SecretNotFoundException, IOException {

        byte[] key = new byte[]{1, 2, 3};
        when(tokenKeyService.fetchAccessTokenKey()).thenReturn(key);
        String token = JWT.create().sign(Algorithm.HMAC256(key));

        assertThatThrownBy(() -> tokenService.verifyAccessTokenAndGetUserId(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldVerifyStaticToken() throws SecretNotFoundException, IOException, InvalidTokenException {

        when(tokenKeyService.fetchStaticTokenKey()).thenReturn(new byte[]{1, 2, 3});

        String token = tokenService.generateStaticTokenForUserId("1");

        assertThat(token).isNotNull();
        assertThat(tokenService.verifyStaticTokenAndGetUserId(token)).isEqualTo("1");
    }

    @Test
    public void shouldFailStaticTokenVerificationIfKeyIsNotFound() throws SecretNotFoundException, IOException {

        when(tokenKeyService.fetchStaticTokenKey()).thenThrow(new SecretNotFoundException());

        //noinspection ResultOfMethodCallIgnored
        assertThatThrownBy(() -> tokenService.verifyStaticTokenAndGetUserId("someToken"));
    }

    @Test
    public void shouldFailStaticTokenVerificationIfTokenIsInvalid() throws SecretNotFoundException, IOException {

        when(tokenKeyService.fetchStaticTokenKey()).thenReturn(new byte[]{});

        assertThatThrownBy(() -> tokenService.verifyStaticTokenAndGetUserId("invalidToken"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailStaticTokenVerificationOnNoSubject() throws SecretNotFoundException, IOException {

        byte[] key = new byte[]{1, 2, 3};
        when(tokenKeyService.fetchStaticTokenKey()).thenReturn(key);
        String token = JWT.create().sign(Algorithm.HMAC256(key));

        assertThatThrownBy(() -> tokenService.verifyStaticTokenAndGetUserId(token))
                .isInstanceOf(InvalidTokenException.class);
    }
}