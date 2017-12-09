package net.dorokhov.pony.web.security.handler;

import net.dorokhov.pony.web.domain.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static net.dorokhov.pony.web.domain.ErrorDto.Code.ACCESS_DENIED;

@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MappingJackson2HttpMessageConverter messageConverter;

    public AccessDeniedHandlerImpl(MappingJackson2HttpMessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        logger.debug("Access denied to '{}'.", request.getServletPath());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        messageConverter.write(new ErrorDto(ACCESS_DENIED, "Access denied."), MediaType.ALL, new ServletServerHttpResponse(response));
    }
}
