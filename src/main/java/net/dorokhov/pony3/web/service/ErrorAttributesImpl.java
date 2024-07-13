package net.dorokhov.pony3.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import net.dorokhov.pony3.web.dto.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.*;
import java.util.stream.Collectors;

import static jakarta.servlet.RequestDispatcher.*;
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
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {

        HttpServletRequest request = (HttpServletRequest) webRequest.resolveReference(REFERENCE_REQUEST);
        Cookie[] cookies = request.getCookies() != null ? request.getCookies() : new Cookie[]{};
        List<String> headers = request.getHeaderNames() != null ? Collections.list(request.getHeaderNames()) : new ArrayList<>();
        Integer statusCode = (Integer) webRequest.getAttribute(ERROR_STATUS_CODE, SCOPE_REQUEST);
        logger.error("Error occurred when executing request: {}.", HttpServletRequestDetails.builder()
                .method(request.getMethod())
                .requestUri((String) webRequest.getAttribute(FORWARD_REQUEST_URI, SCOPE_REQUEST))
                .queryString((String) webRequest.getAttribute(FORWARD_QUERY_STRING, SCOPE_REQUEST))
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
            return super.getErrorAttributes(webRequest, options);
        }

        ErrorDto error;
        if (Objects.equals(statusCode, 404)) {
            error = new ErrorDto()
                    .setCode(ErrorDto.Code.NOT_FOUND)
                    .setMessage("The requested resource is not available.");
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
