package net.dorokhov.pony2.web.security.token;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.web.security.UserDetailsImpl;
import net.dorokhov.pony2.web.security.WebAuthority;
import net.dorokhov.pony2.web.security.token.exception.InvalidTokenException;
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

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static net.dorokhov.pony2.api.user.domain.User.Role.ADMIN;
import static net.dorokhov.pony2.api.user.domain.User.Role.USER;

@Component
public class TokenSecurityContextRepository implements SecurityContextRepository {
    
    private final RequestTokenFinder requestTokenFinder;
    private final TokenService tokenService;
    private final UserService userService;

    public TokenSecurityContextRepository(
            RequestTokenFinder requestTokenFinder,
            TokenService tokenService,
            UserService userService
    ) {
        this.requestTokenFinder = requestTokenFinder;
        this.tokenService = tokenService;
        this.userService = userService;
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
        User user = loadUserByAccessToken(request);
        boolean isAccessToken = user != null;
        if (user == null) {
            user = loadUserByStaticToken(request);
        }
        if (user == null) {
            throw new InvalidTokenException();
        }
        UserDetails userDetails = new UserDetailsImpl(user);
        ImmutableList.Builder<GrantedAuthority> authorities = ImmutableList.<GrantedAuthority>builder()
                .addAll(userDetails.getAuthorities())
                .add(new SimpleGrantedAuthority(WebAuthority.FILE_API.name()));
        if (isAccessToken) {
            if (user.getRoles().contains(USER)) {
                authorities.add(new SimpleGrantedAuthority(WebAuthority.USER_API.name()));
            }
            if (user.getRoles().contains(ADMIN)) {
                authorities.add(new SimpleGrantedAuthority(WebAuthority.ADMIN_API.name()));
            }
        }
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities.build());
    }
    
    @Nullable
    private User loadUserByAccessToken(HttpServletRequest request) throws InvalidTokenException {
        String token = requestTokenFinder.findAccessToken(request);
        if (token != null) {
            return userService.getById(tokenService.verifyAccessTokenAndGetUserId(token)).orElse(null);
        }
        return null;
    }
    
    @Nullable
    private User loadUserByStaticToken(HttpServletRequest request) throws InvalidTokenException {
        String token = requestTokenFinder.findStaticToken(request);
        if (token != null) {
            return userService.getById(tokenService.verifyStaticTokenAndGetUserId(token)).orElse(null);
        }
        return null;
    }
}
