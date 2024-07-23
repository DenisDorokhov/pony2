package net.dorokhov.pony3.web.security.token;

import com.google.common.base.Charsets;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import jakarta.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@Component
public class RequestTokenFinder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String TOKEN_COOKIE_NAME = "pony3.staticToken";
    
    private static final String TOKEN_HEADER_PREFIX = "Bearer ";

    @Nullable
    public String findAccessToken(ServletRequest request) {
        String header = ((HttpServletRequest) request).getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(TOKEN_HEADER_PREFIX)) {
            return header.substring(TOKEN_HEADER_PREFIX.length());
        } else {
            return null;
        }
    }

    @Nullable
    public String findStaticToken(ServletRequest request) {
        Cookie cookie = WebUtils.getCookie((HttpServletRequest) request, TOKEN_COOKIE_NAME);
        if (cookie != null) {
            try {
                return URLDecoder.decode(cookie.getValue(), Charsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                logger.warn("Could not decode cookie value '{}'.", cookie.getValue(), e);
            }
        }
        return null;
    }
}
