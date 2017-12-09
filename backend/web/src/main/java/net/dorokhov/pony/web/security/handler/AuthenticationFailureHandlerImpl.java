package net.dorokhov.pony.web.security.handler;

import net.dorokhov.pony.web.domain.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Component
public class AuthenticationFailureHandlerImpl implements AuthenticationFailureHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MappingJackson2HttpMessageConverter messageConverter;

    public AuthenticationFailureHandlerImpl(MappingJackson2HttpMessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        AuthenticationException exception) throws IOException, ServletException {
        logger.debug("Authentication failed.");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        messageConverter.write(ErrorDto.authenticationFailed(), MediaType.ALL, new ServletServerHttpResponse(response));
    }
}
