package net.dorokhov.pony.web.security.handler;

import net.dorokhov.pony.api.user.domain.User;
import net.dorokhov.pony.web.domain.AuthenticationDto;
import net.dorokhov.pony.web.security.LoginDelegate;
import net.dorokhov.pony.web.security.token.TokenService;
import net.dorokhov.pony.web.service.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserContext userContext;
    private final TokenService tokenService;
    private final MappingJackson2HttpMessageConverter messageConverter;
    private final List<LoginDelegate> loginDelegates;

    public AuthenticationSuccessHandlerImpl(UserContext userContext,
                                            TokenService tokenService,
                                            MappingJackson2HttpMessageConverter messageConverter,
                                            List<LoginDelegate> loginDelegates) {
        this.userContext = userContext;
        this.tokenService = tokenService;
        this.messageConverter = messageConverter;
        this.loginDelegates = loginDelegates;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        User user = userContext.getAuthenticatedUser();
        loginDelegates.forEach(loginDelegate -> loginDelegate.onLogin(user));
        logger.debug("User '{}' has logged in.", user.getId());
        String accessToken = tokenService.generateAccessTokenForUserId(user.getId());
        String staticToken = tokenService.generateStaticTokenForUserId(user.getId());
        messageConverter.write(
                AuthenticationDto.of(user, accessToken, staticToken), 
                MediaType.ALL, new ServletServerHttpResponse(response));
    }
}
