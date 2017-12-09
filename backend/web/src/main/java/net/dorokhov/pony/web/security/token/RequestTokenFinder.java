package net.dorokhov.pony.web.security.token;

import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.annotation.Nullable;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@Component
public class RequestTokenFinder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String TOKEN_COOKIE_NAME = "PONY_TOKEN";
    
    private static final String TOKEN_HEADER_PREFIX = "Bearer ";

    @Nullable
    public String findToken(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String token = findTokenFromHeaders(httpRequest);
        if (token != null) {
            return token;
        }
        if (httpRequest.getMethod().equals(HttpMethod.GET.name()) 
                && httpRequest.getServletPath().startsWith("/api/file/")) {
            token = findTokenFromCookies(httpRequest);
            if (token != null) {
                return token;
            }
        }
        return null;
    }

    @Nullable
    private String findTokenFromHeaders(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(TOKEN_HEADER_PREFIX)) {
            return header.substring(TOKEN_HEADER_PREFIX.length());
        } else {
            return null;
        }
    }

    @Nullable
    private String findTokenFromCookies(HttpServletRequest request) {
        return findCookie(request, TOKEN_COOKIE_NAME);
    }

    @Nullable
    private String findCookie(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
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
