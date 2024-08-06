package net.dorokhov.pony2.web.security;

import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.web.service.UserContext;
import net.dorokhov.pony2.web.service.exception.NotAuthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static net.dorokhov.pony2.test.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class UserContextTest {

    @InjectMocks
    private UserContext userContextService;

    @AfterEach
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