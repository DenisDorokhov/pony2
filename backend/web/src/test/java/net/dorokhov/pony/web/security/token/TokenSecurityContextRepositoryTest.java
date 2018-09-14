package net.dorokhov.pony.web.security.token;

import net.dorokhov.pony.api.user.domain.User;
import net.dorokhov.pony.api.user.service.UserService;
import net.dorokhov.pony.web.security.UserDetailsImpl;
import net.dorokhov.pony.web.security.WebAuthority;
import net.dorokhov.pony.web.security.token.exception.InvalidTokenException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;

import java.util.List;
import java.util.stream.Collectors;

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
    private TokenService tokenService;
    @Mock
    private UserService userService;

    @Test
    public void shouldLoadSecurityContext() throws InvalidTokenException {

        when(requestTokenFinder.findAccessToken(any())).thenReturn("someToken");
        when(tokenService.verifyAccessTokenAndGetUserId("someToken")).thenReturn("1");
        User user = userBuilder()
                .addRoles(User.Role.USER, User.Role.ADMIN)
                .build();
        when(userService.getById("1")).thenReturn(user);

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).satisfies(authentication -> {
            assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(UserDetailsImpl.class, userDetails -> {
                assertThat(userDetails.getUser()).isSameAs(user);
                List<String> authorities = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());
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
        when(userService.getById("1")).thenReturn(null);

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    public void shouldContainContext() throws InvalidTokenException {

        when(requestTokenFinder.findAccessToken(any())).thenReturn("someToken");
        when(tokenService.verifyAccessTokenAndGetUserId("someToken")).thenReturn("1");
        when(userService.getById("1")).thenReturn(user());

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
        User user = userBuilder()
                .addRoles(User.Role.USER, User.Role.ADMIN)
                .build();
        when(userService.getById("1")).thenReturn(user);

        SecurityContext securityContext = tokenSecurityContextRepository.loadContext(httpRequestResponseHolder());

        assertThat(securityContext.getAuthentication()).satisfies(authentication -> {
            assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(UserDetailsImpl.class, userDetails -> {
                assertThat(userDetails.getUser()).isSameAs(user);
                List<String> authorities = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());
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