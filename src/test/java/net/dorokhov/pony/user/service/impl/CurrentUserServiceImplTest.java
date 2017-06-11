package net.dorokhov.pony.user.service.impl;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.repository.UserRepository;
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
public class CurrentUserServiceImplTest {
    
    @InjectMocks
    private CurrentUserServiceImpl currentUserService;

    @Mock
    private TokenManager tokenManager;
    @Mock
    private UserRepository userRepository;

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldGetCurrentUser() throws Exception {
        User user = user();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserDetailsImpl(user), null, null));
        assertThat(currentUserService.getCurrentUser()).isSameAs(user);
    }

    @Test
    public void shouldNotReturnCurrentUserWhenNotAuthenticated() throws Exception {
        assertThat(currentUserService.getCurrentUser()).isNull();
    }

    @Test
    public void shouldAuthenticate() throws Exception {
        when(tokenManager.verifyToken(any())).thenReturn("1");
        User user = user();
        when(userRepository.findOne(1L)).thenReturn(user);
        currentUserService.authenticate("someToken");
        assertThat(currentUserService.getCurrentUser()).isSameAs(user);
    }

    @Test
    public void shouldFailAuthenticationOnInvalidToken() throws Exception {
        when(tokenManager.verifyToken(any())).thenThrow(new InvalidTokenException());
        assertThatThrownBy(() -> currentUserService.authenticate("invalidToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailAuthenticationOnInvalidSubject() throws Exception {
        when(tokenManager.verifyToken(any())).thenReturn("invalidSubject");
        assertThatThrownBy(() -> currentUserService.authenticate("someToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldFailAuthenticationOnNotExistingUser() throws Exception {
        when(tokenManager.verifyToken(any())).thenReturn("1");
        when(userRepository.findOne(1L)).thenReturn(null);
        assertThatThrownBy(() -> currentUserService.authenticate("someToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldLogout() throws Exception {
        User user = user();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserDetailsImpl(user), null, null));
        assertThat(currentUserService.logout()).isSameAs(user);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    public void shouldFailLogoutIfUserIsNotAuthenticated() throws Exception {
        assertThatThrownBy(() -> currentUserService.logout()).isInstanceOf(NotAuthenticatedException.class);
    }
}