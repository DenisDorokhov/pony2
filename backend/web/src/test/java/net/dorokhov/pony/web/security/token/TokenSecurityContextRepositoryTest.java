package net.dorokhov.pony.web.security.token;

import net.dorokhov.pony.web.security.UserDetailsImpl;
import net.dorokhov.pony.web.security.token.exception.InvalidTokenException;
import net.dorokhov.pony.api.user.domain.User;
import net.dorokhov.pony.api.user.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;

import static net.dorokhov.pony.test.UserFixtures.user;
import static net.dorokhov.pony.test.UserFixtures.userBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TokenSecurityContextRepositoryTest {

    @InjectMocks
    private TokenSecurityContextRepository tokenSecurityContextRepository;

    @Mock
    private RequestTokenFinder requestTokenFinder;
    @Mock
    private TokenManager tokenManager;
    @Mock
    private UserService userService;

    @Test
    public void shouldLoadNonemptySecurityContext() throws InvalidTokenException {

        when(requestTokenFinder.findToken(any())).thenReturn("someToken");
        when(tokenManager.verifyAccessTokenAndGetUserId("someToken")).thenReturn("1");
        User user = userBuilder()
                .addRoles(User.Role.USER)
                .build();
        when(userService.getById("1")).thenReturn(user);

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).satisfies(authentication -> {
            assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(UserDetailsImpl.class, userDetails -> {
                assertThat(userDetails.getUser()).isSameAs(user);
                assertThat(authentication.getAuthorities().toArray()).containsAll(userDetails.getAuthorities());
            });
            assertThat(authentication.getCredentials()).isNull();
        });
    }

    @Test
    public void shouldLoadEmptySecurityContextIfNoTokenFound() {

        when(requestTokenFinder.findToken(any())).thenReturn(null);

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldLoadEmptySecurityContextIfTokenIsInvalid() throws InvalidTokenException {

        when(requestTokenFinder.findToken(any())).thenReturn("invalidToken");
        when(tokenManager.verifyAccessTokenAndGetUserId("invalidToken")).thenThrow(new InvalidTokenException());

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldLoadEmptySecurityContextIfUserIsNotFound() throws InvalidTokenException {

        when(requestTokenFinder.findToken(any())).thenReturn("someToken");
        when(tokenManager.verifyAccessTokenAndGetUserId("someToken")).thenReturn("1");
        when(userService.getById("1")).thenReturn(null);

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldContainContext() throws InvalidTokenException {

        when(requestTokenFinder.findToken(any())).thenReturn("someToken");
        when(tokenManager.verifyAccessTokenAndGetUserId("someToken")).thenReturn("1");
        when(userService.getById("1")).thenReturn(user());

        assertThat(tokenSecurityContextRepository.containsContext(new MockHttpServletRequest())).isTrue();
    }

    @Test
    public void shouldNotContainContextIfTokenIsInvalid() {

        when(requestTokenFinder.findToken(any())).thenReturn(null);

        assertThat(tokenSecurityContextRepository.containsContext(new MockHttpServletRequest())).isFalse();
    }

    private HttpRequestResponseHolder httpRequestResponseHolder() {
        return new HttpRequestResponseHolder(new MockHttpServletRequest(), new MockHttpServletResponse());
    }
}