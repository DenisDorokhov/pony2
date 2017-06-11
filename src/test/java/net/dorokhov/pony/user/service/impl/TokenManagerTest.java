package net.dorokhov.pony.user.service.impl;

import net.dorokhov.pony.common.SecretNotFoundException;
import net.dorokhov.pony.user.service.exception.InvalidTokenException;
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
    public void shouldSignAndVerifyToken() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");
        String token = tokenManager.signToken("someSubject");
        assertThat(token).isNotNull();
        assertThat(tokenManager.verifyToken(token)).isEqualTo("someSubject");
    }

    @Test
    public void shouldFailVerificationOnInvalidToken() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenReturn("someSecret");
        assertThatThrownBy(() -> tokenManager.verifyToken("invalidToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailVerificationOnNotFoundSecret() throws Exception {
        when(tokenSecretManager.fetchTokenSecret()).thenThrow(new SecretNotFoundException());
        assertThatThrownBy(() -> tokenManager.verifyToken("someToken"));
    }
}