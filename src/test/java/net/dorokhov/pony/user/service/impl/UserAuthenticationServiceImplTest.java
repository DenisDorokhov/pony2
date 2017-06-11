package net.dorokhov.pony.user.service.impl;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.domain.UserToken;
import net.dorokhov.pony.user.service.exception.InvalidCredentialsException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static net.dorokhov.pony.fixture.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserAuthenticationServiceImplTest {

    @InjectMocks
    private UserAuthenticationServiceImpl userAuthenticationService;

    @Mock
    private TokenManager tokenManager;
    @Mock
    private AuthenticationManager authenticationManager;

    @Test
    public void shouldAuthenticate() throws Exception {
        User existingUser = user();
        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        when(tokenManager.signToken(any())).thenReturn("someToken");
        UserToken userToken = userAuthenticationService.authenticate("someEmail", "somePassword");
        assertThat(userToken.getUser()).isSameAs(existingUser);
        assertThat(userToken.getToken()).isEqualTo("someToken");
    }

    @Test
    public void shouldFailAuthenticationOnInvalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new AuthenticationCredentialsNotFoundException("Credentials not found."));
        assertThatThrownBy(() -> userAuthenticationService.authenticate("someEmail", "somePassword")).isInstanceOf(InvalidCredentialsException.class);
    }
}