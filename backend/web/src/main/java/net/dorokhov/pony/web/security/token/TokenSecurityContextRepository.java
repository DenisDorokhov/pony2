package net.dorokhov.pony.web.security.token;

import net.dorokhov.pony.api.user.domain.User;
import net.dorokhov.pony.api.user.service.UserService;
import net.dorokhov.pony.web.security.token.exception.InvalidTokenException;
import net.dorokhov.pony.web.security.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class TokenSecurityContextRepository implements SecurityContextRepository {
    
    private final RequestTokenFinder requestTokenFinder;
    private final TokenManager tokenManager;
    private final UserService userService;

    public TokenSecurityContextRepository(RequestTokenFinder requestTokenFinder,
                                          TokenManager tokenManager,
                                          UserService userService) {
        this.requestTokenFinder = requestTokenFinder;
        this.tokenManager = tokenManager;
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
            loadUser(request);
            return true;
        } catch (InvalidTokenException e) {
            return false;
        }
    }
    
    private SecurityContext loadContext(HttpServletRequest request) throws InvalidTokenException {
        UserDetailsImpl userDetails = new UserDetailsImpl(loadUser(request));
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        return securityContext;
    }
    
    private User loadUser(HttpServletRequest request) throws InvalidTokenException {
        String token = requestTokenFinder.findToken(request);
        if (token == null) {
            throw new InvalidTokenException();
        }
        User user = userService.getById(tokenManager.verifyToken(token));
        if (user == null) {
            throw new InvalidTokenException();
        }
        return user;
    }
}
