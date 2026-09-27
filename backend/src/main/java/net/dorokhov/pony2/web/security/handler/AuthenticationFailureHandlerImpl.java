package net.dorokhov.pony2.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.web.WebConfig;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.security.BruteForceProtector;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
public class AuthenticationFailureHandlerImpl implements AuthenticationFailureHandler {

    private final BruteForceProtector bruteForceProtector;
    private final JsonMapper jsonMapper;

    public AuthenticationFailureHandlerImpl(
            BruteForceProtector bruteForceProtector,
            JsonMapper jsonMapper
    ) {
        this.bruteForceProtector = bruteForceProtector;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        bruteForceProtector.onFailedLoginAttempt(request, request.getParameter(WebConfig.AUTH_PARAM_USERNAME));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), ErrorDto.authenticationFailed());
    }
}
