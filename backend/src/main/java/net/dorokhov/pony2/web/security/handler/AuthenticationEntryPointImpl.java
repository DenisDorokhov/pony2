package net.dorokhov.pony2.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.web.dto.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MappingJackson2HttpMessageConverter messageConverter;

    public AuthenticationEntryPointImpl(MappingJackson2HttpMessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        logger.debug("Access denied to '{}'.", request.getServletPath());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        messageConverter.write(ErrorDto.authenticationFailed(), MediaType.ALL, new ServletServerHttpResponse(response));
    }
}
