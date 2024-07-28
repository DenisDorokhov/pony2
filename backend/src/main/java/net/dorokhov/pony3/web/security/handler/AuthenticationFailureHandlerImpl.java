package net.dorokhov.pony3.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony3.web.WebConfig;
import net.dorokhov.pony3.web.dto.ErrorDto;
import net.dorokhov.pony3.web.security.BruteForceProtector;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationFailureHandlerImpl implements AuthenticationFailureHandler {

    private final BruteForceProtector bruteForceProtector;
    private final MappingJackson2HttpMessageConverter messageConverter;

    public AuthenticationFailureHandlerImpl(
            BruteForceProtector bruteForceProtector,
            MappingJackson2HttpMessageConverter messageConverter
    ) {
        this.bruteForceProtector = bruteForceProtector;
        this.messageConverter = messageConverter;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        bruteForceProtector.onFailedLoginAttempt(request, request.getParameter(WebConfig.AUTH_PARAM_USERNAME));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        messageConverter.write(ErrorDto.authenticationFailed(), MediaType.ALL, new ServletServerHttpResponse(response));
    }
}
