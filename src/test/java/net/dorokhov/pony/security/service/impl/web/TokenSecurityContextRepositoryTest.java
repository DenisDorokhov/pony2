package net.dorokhov.pony.security.service.impl.web;

import net.dorokhov.pony.security.service.impl.token.TokenManager;
import net.dorokhov.pony.security.service.impl.token.exception.InvalidTokenException;
import net.dorokhov.pony.security.service.impl.userdetails.UserDetailsImpl;
import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;

import static net.dorokhov.pony.fixture.UserFixtures.user;
import static net.dorokhov.pony.fixture.UserFixtures.userBuilder;
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
    public void shouldLoadNonemptySecurityContext() throws Exception {
        when(requestTokenFinder.findToken(any())).thenReturn("someToken");
        when(tokenManager.verifyToken("someToken")).thenReturn(1L);
        User user = userBuilder()
                .addRoles(User.Role.USER)
                .build();
        when(userService.getById(1L)).thenReturn(user);
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
    public void shouldLoadEmptySecurityContextIfNoTokenFound() throws Exception {
        when(requestTokenFinder.findToken(any())).thenReturn(null);
        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());
        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldLoadEmptySecurityContextIfTokenIsInvalid() throws Exception {
        when(requestTokenFinder.findToken(any())).thenReturn("invalidToken");
        when(tokenManager.verifyToken("invalidToken")).thenThrow(new InvalidTokenException());
        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());
        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldLoadEmptySecurityContextIfUserIsNotFound() throws Exception {
        when(requestTokenFinder.findToken(any())).thenReturn("someToken");
        when(tokenManager.verifyToken("someToken")).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(null);
        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());
        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldContainContext() throws Exception {
        when(requestTokenFinder.findToken(any())).thenReturn("someToken");
        when(tokenManager.verifyToken("someToken")).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(user());
        assertThat(tokenSecurityContextRepository.containsContext(new MockHttpServletRequest())).isTrue();
    }

    @Test
    public void shouldNotContainContextIfTokenIsInvalid() throws Exception {
        when(requestTokenFinder.findToken(any())).thenReturn(null);
        assertThat(tokenSecurityContextRepository.containsContext(new MockHttpServletRequest())).isFalse();
    }

    private HttpRequestResponseHolder httpRequestResponseHolder() {
        return new HttpRequestResponseHolder(new MockHttpServletRequest(), new MockHttpServletResponse());
    }
}