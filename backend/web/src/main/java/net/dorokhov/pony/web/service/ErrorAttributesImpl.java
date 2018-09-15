package net.dorokhov.pony.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dorokhov.pony.web.domain.ErrorDto;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;

import static javax.servlet.RequestDispatcher.ERROR_STATUS_CODE;
import static org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

@Component
public class ErrorAttributesImpl extends DefaultErrorAttributes {
    
    private final ObjectMapper objectMapper;

    public ErrorAttributesImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getErrorAttributes(RequestAttributes requestAttributes, boolean includeStackTrace) {

        HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(REFERENCE_REQUEST);
        String acceptedMimeType = request.getHeader(HttpHeaders.ACCEPT);
        if (acceptedMimeType != null
                && !acceptedMimeType.equals(MediaType.ALL_VALUE)
                && !acceptedMimeType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            return super.getErrorAttributes(requestAttributes, includeStackTrace);
        }

        ErrorDto error;
        Integer status = (Integer) requestAttributes.getAttribute(ERROR_STATUS_CODE, SCOPE_REQUEST);
        if (Objects.equals(status, 404)) {
            error = new ErrorDto(ErrorDto.Code.NOT_FOUND, "The requested resource is not available.");
        } else {
            error = ErrorDto.unexpected();
        }
        return objectMapper.convertValue(error, Map.class);
    }
}
