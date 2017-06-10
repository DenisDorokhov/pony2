package net.dorokhov.pony.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dorokhov.pony.web.domain.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static net.dorokhov.pony.web.domain.ErrorDto.Code.ACCESS_DENIED;

@Component
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper objectMapper;

    public UnauthorizedEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.trace("Access denied to '{}'.", request.getServletPath());
        if (request.getServletPath().startsWith("/api/")) {
            ErrorDto error = new ErrorDto(ACCESS_DENIED, "Access denied.");
            response.setContentType("application/json");
            response.setStatus(SC_UNAUTHORIZED);
            response.getOutputStream().print(objectMapper.writeValueAsString(error));
        } else {
            response.sendError(SC_UNAUTHORIZED);
        }
    }
}
