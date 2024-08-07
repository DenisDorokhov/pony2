package net.dorokhov.pony2.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.web.dto.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static net.dorokhov.pony2.web.dto.ErrorDto.Code.ACCESS_DENIED;

@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MappingJackson2HttpMessageConverter messageConverter;

    public AccessDeniedHandlerImpl(MappingJackson2HttpMessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        logger.debug("Access denied to '{}'.", request.getServletPath());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        messageConverter.write(
                new ErrorDto()
                        .setCode(ACCESS_DENIED)
                        .setMessage("Access denied."),
                MediaType.ALL, new ServletServerHttpResponse(response));
    }
}
