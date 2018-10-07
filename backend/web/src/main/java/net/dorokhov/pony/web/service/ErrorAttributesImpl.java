package net.dorokhov.pony.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import net.dorokhov.pony.web.domain.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static javax.servlet.RequestDispatcher.ERROR_STATUS_CODE;
import static javax.servlet.RequestDispatcher.FORWARD_QUERY_STRING;
import static javax.servlet.RequestDispatcher.FORWARD_REQUEST_URI;
import static org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST;
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

        HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(REFERENCE_REQUEST);
        Cookie[] cookies = request.getCookies() != null ? request.getCookies() : new Cookie[]{};
        List<String> headers = request.getHeaderNames() != null ? Collections.list(request.getHeaderNames()) : new ArrayList<>();
        Integer statusCode = (Integer) requestAttributes.getAttribute(ERROR_STATUS_CODE, SCOPE_REQUEST);
        logger.error("Error occurred when executing request: {}.", HttpServletRequestDetails.builder()
                .method(request.getMethod())
                .requestUri((String) requestAttributes.getAttribute(FORWARD_REQUEST_URI, SCOPE_REQUEST))
                .queryString((String) requestAttributes.getAttribute(FORWARD_QUERY_STRING, SCOPE_REQUEST))
                .cookies(Arrays.stream(cookies)
                        .collect(Collectors.toMap(Cookie::getName, Cookie::getValue)))
                .headers(headers.stream()
                        .map(name -> new AbstractMap.SimpleEntry<>(name, request.getHeader(name)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .statusCode(statusCode)
                .build());
        
        String acceptedMimeType = request.getHeader(HttpHeaders.ACCEPT);
        if (acceptedMimeType != null
                && !acceptedMimeType.equals(MediaType.ALL_VALUE)
                && !acceptedMimeType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            return super.getErrorAttributes(requestAttributes, includeStackTrace);
        }

        ErrorDto error;
        if (Objects.equals(statusCode, 404)) {
            error = new ErrorDto(ErrorDto.Code.NOT_FOUND, "The requested resource is not available.");
        } else {
            error = ErrorDto.unexpected();
        }
        return objectMapper.convertValue(error, Map.class);
    }
    
    private static class HttpServletRequestDetails {
        
        private final String method;
        private final String requestUri;
        private final String queryString;
        private final Map<String, String> cookies;
        private final Map<String, String> headers;
        private final Integer statusCode;

        private HttpServletRequestDetails(Builder builder) {
            method = builder.method;
            requestUri = builder.requestUri;
            queryString = builder.queryString;
            cookies = builder.cookies;
            headers = builder.headers;
            statusCode = builder.statusCode;
        }

        public String getMethod() {
            return method;
        }

        public String getRequestUri() {
            return requestUri;
        }

        public String getQueryString() {
            return queryString;
        }

        public Map<String, String> getCookies() {
            return cookies;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("method", method)
                    .add("requestUri", requestUri)
                    .add("queryString", queryString)
                    .add("cookies", cookies)
                    .add("headers", headers)
                    .add("statusCode", statusCode)
                    .toString();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private String method;
            private String requestUri;
            private String queryString;
            private Map<String, String> cookies;
            private Map<String, String> headers;
            private Integer statusCode;

            private Builder() {
            }

            public Builder method(String method) {
                this.method = method;
                return this;
            }

            public Builder requestUri(String requestUri) {
                this.requestUri = requestUri;
                return this;
            }

            public Builder queryString(String queryString) {
                this.queryString = queryString;
                return this;
            }

            public Builder cookies(Map<String, String> cookies) {
                this.cookies = cookies;
                return this;
            }

            public Builder headers(Map<String, String> headers) {
                this.headers = headers;
                return this;
            }

            public Builder statusCode(Integer statusCode) {
                this.statusCode = statusCode;
                return this;
            }

            public HttpServletRequestDetails build() {
                return new HttpServletRequestDetails(this);
            }
        }
    }
}
