package net.dorokhov.pony2.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

import static net.dorokhov.pony2.web.service.OpenSubsonicResponseService.ERROR_UNAUTHORIZED;

@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JsonMapper jsonMapper;
    private final OpenSubsonicResponseService openSubsonicResponseService;

    public AccessDeniedHandlerImpl(
            JsonMapper jsonMapper,
            OpenSubsonicResponseService openSubsonicResponseService
    ) {
        this.jsonMapper = jsonMapper;
        this.openSubsonicResponseService = openSubsonicResponseService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        logger.debug("Access denied to '{}'.", request.getServletPath());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (openSubsonicResponseService.isOpenSubsonicRequest(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            jsonMapper.writeValue(response.getOutputStream(), openSubsonicResponseService.createError(ERROR_UNAUTHORIZED, "Access denied."));
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            jsonMapper.writeValue(response.getOutputStream(), ErrorDto.accessDenied());
        }
    }
}
