package net.dorokhov.pony2.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

import static net.dorokhov.pony2.web.service.OpenSubsonicResponseService.ERROR_INVALID_API_KEY;

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JsonMapper jsonMapper;
    private final OpenSubsonicResponseService openSubsonicResponseService;

    public AuthenticationEntryPointImpl(
            JsonMapper jsonMapper,
            OpenSubsonicResponseService openSubsonicResponseService
    ) {
        this.jsonMapper = jsonMapper;
        this.openSubsonicResponseService = openSubsonicResponseService;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        logger.debug("Access denied to '{}'.", request.getServletPath());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (openSubsonicResponseService.isOpenSubsonicRequest(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            jsonMapper.writeValue(response.getOutputStream(), openSubsonicResponseService.createError(ERROR_INVALID_API_KEY, "Access denied."));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonMapper.writeValue(response.getOutputStream(), ErrorDto.authenticationFailed());
        }
    }
}
