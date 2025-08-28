package net.dorokhov.pony2.web.security.token;

import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.web.security.UserDetailsImpl;
import net.dorokhov.pony2.web.security.WebAuthority;
import net.dorokhov.pony2.web.security.token.exception.InvalidTokenException;
import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import static net.dorokhov.pony2.api.user.domain.User.Role.ADMIN;
import static net.dorokhov.pony2.api.user.domain.User.Role.USER;

@Component
public class TokenSecurityContextRepository implements SecurityContextRepository {
    
    private final RequestTokenFinder requestTokenFinder;
    private final TokenService tokenService;
    private final UserService userService;
    private final OpenSubsonicResponseService openSubsonicResponseService;

    public TokenSecurityContextRepository(
            RequestTokenFinder requestTokenFinder,
            TokenService tokenService,
            UserService userService,
            OpenSubsonicResponseService openSubsonicResponseService
    ) {
        this.requestTokenFinder = requestTokenFinder;
        this.tokenService = tokenService;
        this.userService = userService;
        this.openSubsonicResponseService = openSubsonicResponseService;
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        SecurityContext securityContext;
        try {
            securityContext = loadContext(requestResponseHolder.getRequest());
        } catch (InvalidTokenException e) {
            securityContext = SecurityContextHolder.createEmptyContext();
        }
        return securityContext;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        // Do nothing.
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        try {
            loadAuthentication(request);
            return true;
        } catch (InvalidTokenException e) {
            return false;
        }
    }
    
    private SecurityContext loadContext(HttpServletRequest request) throws InvalidTokenException {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(loadAuthentication(request));
        return securityContext;
    }
    
    private Authentication loadAuthentication(HttpServletRequest request) throws InvalidTokenException {
        AuthenticationSource source;
        User user = loadUserByAccessToken(request);
        if (user != null) {
            source = AuthenticationSource.ACCESS_TOKEN;
        } else {
            user = loadUserBySubsonicCredentials(request);
            if (user != null) {
                source = AuthenticationSource.OPEN_SUBSONIC;
            } else {
                user = loadUserByStaticToken(request);
                if (user != null) {
                    source = AuthenticationSource.STATIC_TOKEN;
                } else {
                    throw new InvalidTokenException();
                }
            }
        }
        UserDetails userDetails = new UserDetailsImpl(user);
        ImmutableSet.Builder<GrantedAuthority> authorities = ImmutableSet.<GrantedAuthority>builder()
                .addAll(userDetails.getAuthorities());
        switch (source) {
            case ACCESS_TOKEN -> {
                addAuthority(authorities, WebAuthority.OPEN_SUBSONIC_API);
                addAuthority(authorities, WebAuthority.FILE_API);
                if (user.getRoles().contains(USER)) {
                    addAuthority(authorities, WebAuthority.USER_API);
                }
                if (user.getRoles().contains(ADMIN)) {
                    addAuthority(authorities, WebAuthority.ADMIN_API);
                }
            }
            case OPEN_SUBSONIC ->
                    addAuthority(authorities, WebAuthority.OPEN_SUBSONIC_API);
            case STATIC_TOKEN ->
                    addAuthority(authorities, WebAuthority.FILE_API);
        }
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities.build());
    }

    private void addAuthority(ImmutableSet.Builder<GrantedAuthority> authorities, WebAuthority authority) {
        authorities.add(new SimpleGrantedAuthority(authority.name()));
    }

    private @Nullable User loadUserByAccessToken(HttpServletRequest request) throws InvalidTokenException {
        String token = requestTokenFinder.findAccessToken(request);
        if (token != null) {
            return userService.getById(tokenService.verifyAccessTokenAndGetUserId(token)).orElse(null);
        }
        return null;
    }

    private @Nullable User loadUserByStaticToken(HttpServletRequest request) throws InvalidTokenException {
        String token = requestTokenFinder.findStaticToken(request);
        if (token != null) {
            return userService.getById(tokenService.verifyStaticTokenAndGetUserId(token)).orElse(null);
        }
        return null;
    }

    private @Nullable User loadUserBySubsonicCredentials(HttpServletRequest request) throws InvalidTokenException {
        if (openSubsonicResponseService.isOpenSubsonicRequest(request)) {
            String apiKey = requestTokenFinder.findOpenSubsonicApiKey(request);
            if (apiKey != null) {
                return userService.getById(tokenService.verifyOpenSubsonicApiKeyAndGetUserId(apiKey)).orElse(null);
            }
        }
        return null;
    }

    enum AuthenticationSource {
        ACCESS_TOKEN,
        STATIC_TOKEN,
        OPEN_SUBSONIC,
    }
}
