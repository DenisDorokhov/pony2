package net.dorokhov.pony3.web.security.token;

import com.google.common.collect.Sets;
import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.api.user.service.UserService;
import net.dorokhov.pony3.web.security.UserDetailsImpl;
import net.dorokhov.pony3.web.security.WebAuthority;
import net.dorokhov.pony3.web.security.token.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;

import java.util.List;
import java.util.Optional;

import static net.dorokhov.pony3.test.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TokenSecurityContextRepositoryTest {

    @InjectMocks
    private TokenSecurityContextRepository tokenSecurityContextRepository;

    @Mock
    private RequestTokenFinder requestTokenFinder;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserService userService;

    @Test
    public void shouldLoadSecurityContext() throws InvalidTokenException {

        when(requestTokenFinder.findAccessToken(any())).thenReturn("someToken");
        when(tokenService.verifyAccessTokenAndGetUserId("someToken")).thenReturn("1");
        User user = user()
                .setRoles(Sets.newHashSet(User.Role.USER, User.Role.ADMIN));
        when(userService.getById("1")).thenReturn(Optional.of(user));

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).satisfies(authentication -> {
            assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(UserDetailsImpl.class, userDetails -> {
                assertThat(userDetails.getUser()).isSameAs(user);
                List<String> authorities = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();
                assertThat(authorities).containsExactlyInAnyOrder(
                        "ROLE_" + User.Role.USER.name(), 
                        "ROLE_" + User.Role.ADMIN.name(), 
                        WebAuthority.FILE_API.name(),
                        WebAuthority.USER_API.name(),
                        WebAuthority.ADMIN_API.name()
                );
            });
            assertThat(authentication.getCredentials()).isNull();
        });
    }

    @Test
    public void shouldLoadEmptySecurityContextIfNoTokenFound() {

        when(requestTokenFinder.findAccessToken(any())).thenReturn(null);
        when(requestTokenFinder.findStaticToken(any())).thenReturn(null);

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldLoadEmptySecurityContextIfTokenIsInvalid() throws InvalidTokenException {

        when(requestTokenFinder.findAccessToken(any())).thenReturn("invalidToken");
        when(tokenService.verifyAccessTokenAndGetUserId("invalidToken")).thenThrow(new InvalidTokenException());

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldLoadEmptySecurityContextIfUserIsNotFound() throws InvalidTokenException {

        when(requestTokenFinder.findAccessToken(any())).thenReturn("someToken");
        when(tokenService.verifyAccessTokenAndGetUserId("someToken")).thenReturn("1");
        when(userService.getById("1")).thenReturn(Optional.empty());

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldContainContext() throws InvalidTokenException {

        when(requestTokenFinder.findAccessToken(any())).thenReturn("someToken");
        when(tokenService.verifyAccessTokenAndGetUserId("someToken")).thenReturn("1");
        when(userService.getById("1")).thenReturn(Optional.of(user()));

        assertThat(tokenSecurityContextRepository.containsContext(new MockHttpServletRequest())).isTrue();
    }

    @Test
    public void shouldNotContainContextIfNoTokenFound() {

        when(requestTokenFinder.findAccessToken(any())).thenReturn(null);
        when(requestTokenFinder.findStaticToken(any())).thenReturn(null);

        assertThat(tokenSecurityContextRepository.containsContext(new MockHttpServletRequest())).isFalse();
    }

    @Test
    public void shouldLoadSecurityContextFromStatucToken() throws InvalidTokenException {

        when(requestTokenFinder.findAccessToken(any())).thenReturn(null);
        when(requestTokenFinder.findStaticToken(any())).thenReturn("someToken");

        when(tokenService.verifyStaticTokenAndGetUserId("someToken")).thenReturn("1");
        User user = user()
                .setRoles(Sets.newHashSet(User.Role.USER, User.Role.ADMIN));
        when(userService.getById("1")).thenReturn(Optional.of(user));

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).satisfies(authentication -> {
            assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(UserDetailsImpl.class, userDetails -> {
                assertThat(userDetails.getUser()).isSameAs(user);
                List<String> authorities = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();
                assertThat(authorities).containsExactlyInAnyOrder(
                        "ROLE_" + User.Role.USER.name(),
                        "ROLE_" + User.Role.ADMIN.name(),
                        WebAuthority.FILE_API.name()
                );
            });
            assertThat(authentication.getCredentials()).isNull();
        });
    }

    private HttpRequestResponseHolder httpRequestResponseHolder() {
        return new HttpRequestResponseHolder(new MockHttpServletRequest(), new MockHttpServletResponse());
    }
}