package net.dorokhov.pony3.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.web.dto.AuthenticationDto;
import net.dorokhov.pony3.web.security.BruteForceProtector;
import net.dorokhov.pony3.web.security.LoginDelegate;
import net.dorokhov.pony3.web.security.token.TokenService;
import net.dorokhov.pony3.web.service.UserContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    private final UserContext userContext;
    private final TokenService tokenService;
    private final BruteForceProtector bruteForceProtector;
    private final MappingJackson2HttpMessageConverter messageConverter;
    private final List<LoginDelegate> loginDelegates;

    public AuthenticationSuccessHandlerImpl(
            UserContext userContext,
            TokenService tokenService,
            BruteForceProtector bruteForceProtector,
            MappingJackson2HttpMessageConverter messageConverter,
            List<LoginDelegate> loginDelegates
    ) {
        this.userContext = userContext;
        this.tokenService = tokenService;
        this.bruteForceProtector = bruteForceProtector;
        this.messageConverter = messageConverter;
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
        messageConverter.write(
                AuthenticationDto.of(user, accessToken, staticToken),
                MediaType.ALL, new ServletServerHttpResponse(response));
    }
}
