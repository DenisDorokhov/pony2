package net.dorokhov.pony2.web.security.token;

import com.google.common.base.Charsets;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import jakarta.annotation.Nullable;

import java.net.URLDecoder;

@Component
public class RequestTokenFinder {

    public static final String TOKEN_COOKIE_NAME = "pony2.staticToken";
    public static final String TOKEN_PARAM_NAME = "apiKey";

    private static final String TOKEN_HEADER_PREFIX = "Bearer ";

    public @Nullable String findAccessToken(ServletRequest request) {
        String header = ((HttpServletRequest) request).getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(TOKEN_HEADER_PREFIX)) {
            return header.substring(TOKEN_HEADER_PREFIX.length());
        } else {
            return null;
        }
    }

    public @Nullable String findStaticToken(ServletRequest request) {
        Cookie cookie = WebUtils.getCookie((HttpServletRequest) request, TOKEN_COOKIE_NAME);
        if (cookie != null) {
            return URLDecoder.decode(cookie.getValue(), Charsets.UTF_8);
        }
        return request.getParameter(TOKEN_PARAM_NAME);
    }

    public @Nullable String findOpenSubsonicApiKey(ServletRequest request) {
        return request.getParameter(TOKEN_PARAM_NAME);
    }
}
