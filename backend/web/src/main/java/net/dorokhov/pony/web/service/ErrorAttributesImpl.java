package net.dorokhov.pony.web.service;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dorokhov.pony.web.domain.ErrorDto;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;

@Component
public class ErrorAttributesImpl extends DefaultErrorAttributes {

    private final ObjectMapper objectMapper;

    public ErrorAttributesImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getErrorAttributes(RequestAttributes requestAttributes, boolean includeStackTrace) {
        ErrorDto error;
        Integer status = (Integer) requestAttributes.getAttribute("javax.servlet.error.status_code", RequestAttributes.SCOPE_REQUEST);
        if (status == 404) {
            error = new ErrorDto(ErrorDto.Code.NOT_FOUND, "The requested resource is not available.");
        } else {
            error = ErrorDto.unexpected();
        }
        return objectMapper.convertValue(error, Map.class);
    }
}
