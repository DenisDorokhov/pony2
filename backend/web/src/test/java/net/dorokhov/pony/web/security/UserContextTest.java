package net.dorokhov.pony.web.security;

import net.dorokhov.pony.web.service.exception.NotAuthenticatedException;
import net.dorokhov.pony.api.user.domain.User;
import net.dorokhov.pony.web.service.UserContext;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static net.dorokhov.pony.test.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class UserContextTest {

    @InjectMocks
    private UserContext userContextService;

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldGetCurrentUser() {

        User user = user();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(new UserDetailsImpl(user), null));

        assertThat(userContextService.getAuthenticatedUser()).isSameAs(user);
    }

    @Test
    public void shouldNotReturnCurrentUserWhenNotAuthenticated() {
        assertThatThrownBy(() -> userContextService.getAuthenticatedUser()).isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    public void shouldCheckIfUserIsAuthenticated() {

        assertThat(userContextService.isUserAuthenticated()).isFalse();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(new UserDetailsImpl(user()), null));

        assertThat(userContextService.isUserAuthenticated()).isTrue();
    }
}