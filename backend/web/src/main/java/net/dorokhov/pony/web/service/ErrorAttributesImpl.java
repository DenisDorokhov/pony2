package net.dorokhov.pony.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dorokhov.pony.web.domain.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;

import javax.servlet.ServletException;
import java.util.Map;

import static javax.servlet.RequestDispatcher.ERROR_STATUS_CODE;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

@Component
public class ErrorAttributesImpl extends DefaultErrorAttributes {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper objectMapper;

    public ErrorAttributesImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getErrorAttributes(RequestAttributes requestAttributes, boolean includeStackTrace) {
        ErrorDto error;
        Integer status = (Integer) requestAttributes.getAttribute(ERROR_STATUS_CODE, SCOPE_REQUEST);
        if (status == 404) {
            error = new ErrorDto(ErrorDto.Code.NOT_FOUND, "The requested resource is not available.");
        } else {
            Throwable throwable = getError(requestAttributes);
            if (throwable != null) {
                while (throwable instanceof ServletException && throwable.getCause() != null) {
                    throwable = throwable.getCause();
                }
            }
            logger.error("Unexpected error of status {} occurred.", status, throwable);
            error = ErrorDto.unexpected();
        }
        return objectMapper.convertValue(error, Map.class);
    }
}
