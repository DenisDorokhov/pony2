package net.dorokhov.pony.web.security.handler;

import net.dorokhov.pony.web.domain.AuthenticationDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.security.token.TokenManager;
import net.dorokhov.pony.web.service.UserContextService;
import net.dorokhov.pony.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserContextService userContextService;
    private final TokenManager tokenManager;
    private final MappingJackson2HttpMessageConverter messageConverter;

    public AuthenticationSuccessHandlerImpl(UserContextService userContextService,
                                            TokenManager tokenManager,
                                            MappingJackson2HttpMessageConverter messageConverter) {
        this.userContextService = userContextService;
        this.tokenManager = tokenManager;
        this.messageConverter = messageConverter;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        User user = userContextService.getAuthenticatedUser();
        logger.debug("User '{}' has logged in.", user.getId());
        String token = tokenManager.createToken(user.getId().toString());
        messageConverter.write(new AuthenticationDto(new UserDto(user), token), MediaType.ALL, new ServletServerHttpResponse(response));
    }
}
