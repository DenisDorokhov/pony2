package net.dorokhov.pony2.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static net.dorokhov.pony2.web.service.OpenSubsonicResponseService.ERROR_INVALID_API_KEY;
import static net.dorokhov.pony2.web.service.OpenSubsonicResponseService.ERROR_UNAUTHORIZED;

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MappingJackson2HttpMessageConverter messageConverter;
    private final OpenSubsonicResponseService openSubsonicResponseService;

    public AuthenticationEntryPointImpl(
            MappingJackson2HttpMessageConverter messageConverter,
            OpenSubsonicResponseService openSubsonicResponseService
    ) {
        this.messageConverter = messageConverter;
        this.openSubsonicResponseService = openSubsonicResponseService;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        logger.debug("Access denied to '{}'.", request.getServletPath());
        if (openSubsonicResponseService.isOpenSubsonicRequest(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            messageConverter.write(openSubsonicResponseService.createError(ERROR_INVALID_API_KEY, "Access denied."), MediaType.ALL, new ServletServerHttpResponse(response));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            messageConverter.write(ErrorDto.authenticationFailed(), MediaType.ALL, new ServletServerHttpResponse(response));
        }
    }
}
