package net.dorokhov.pony.user.service.impl;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.exception.InvalidTokenException;
import net.dorokhov.pony.user.service.exception.NotAuthenticatedException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static net.dorokhov.pony.fixture.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserContextServiceImplTest {
    
    @InjectMocks
    private UserContextServiceImpl currentUserService;

    @Mock
    private TokenManager tokenManager;
    @Mock
    private UserService userService;

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldGetCurrentUser() throws Exception {
        User user = user();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserDetailsImpl(user), null, null));
        assertThat(currentUserService.getUser()).isSameAs(user);
    }

    @Test
    public void shouldNotReturnCurrentUserWhenNotAuthenticated() throws Exception {
        assertThat(currentUserService.getUser()).isNull();
    }

    @Test
    public void shouldAuthenticate() throws Exception {
        when(tokenManager.verifyToken(any())).thenReturn("1");
        User user = user();
        when(userService.getById(1L)).thenReturn(user);
        currentUserService.setUserFromToken("someToken");
        assertThat(currentUserService.getUser()).isSameAs(user);
    }

    @Test
    public void shouldFailAuthenticationOnInvalidToken() throws Exception {
        when(tokenManager.verifyToken(any())).thenThrow(new InvalidTokenException());
        assertThatThrownBy(() -> currentUserService.setUserFromToken("invalidToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailAuthenticationOnInvalidSubject() throws Exception {
        when(tokenManager.verifyToken(any())).thenReturn("invalidSubject");
        assertThatThrownBy(() -> currentUserService.setUserFromToken("someToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailAuthenticationOnNotExistingUser() throws Exception {
        when(tokenManager.verifyToken(any())).thenReturn("1");
        when(userService.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> currentUserService.setUserFromToken("someToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldLogout() throws Exception {
        User user = user();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserDetailsImpl(user), null, null));
        assertThat(currentUserService.clearUser()).isSameAs(user);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    public void shouldFailLogoutIfUserIsNotAuthenticated() throws Exception {
        assertThatThrownBy(() -> currentUserService.clearUser()).isInstanceOf(NotAuthenticatedException.class);
    }
}