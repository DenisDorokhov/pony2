package net.dorokhov.pony2.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.security.BruteForceProtector;
import net.dorokhov.pony2.web.security.LoginDelegate;
import net.dorokhov.pony2.web.security.token.TokenService;
import net.dorokhov.pony2.web.service.UserContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

@Component
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    private final UserContext userContext;
    private final TokenService tokenService;
    private final BruteForceProtector bruteForceProtector;
    private final JsonMapper jsonMapper;
    private final List<LoginDelegate> loginDelegates;

    public AuthenticationSuccessHandlerImpl(
            UserContext userContext,
            TokenService tokenService,
            BruteForceProtector bruteForceProtector,
            JsonMapper jsonMapper,
            List<LoginDelegate> loginDelegates
    ) {
        this.userContext = userContext;
        this.tokenService = tokenService;
        this.bruteForceProtector = bruteForceProtector;
        this.jsonMapper = jsonMapper;
        this.loginDelegates = loginDelegates;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        User user = userContext.getAuthenticatedUser();
        loginDelegates.forEach(loginDelegate -> loginDelegate.onLogin(user));
        bruteForceProtector.onSuccessfulLoginAttempt(request, user.getEmail());
        String accessToken = tokenService.generateAccessTokenForUserId(user.getId());
        String staticToken = tokenService.generateStaticTokenForUserId(user.getId());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), AuthenticationDto.of(user, accessToken, staticToken));
    }
}
