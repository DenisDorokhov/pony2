package net.dorokhov.pony.web.security;

import com.google.common.base.Charsets;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.exception.InvalidTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.WebUtils;

import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@Component
public class UserTokenFilter extends GenericFilterBean {
    
    private static final String TOKEN_HEADER_PREFIX = "Bearer ";
    private static final String TOKEN_COOKIE_NAME = "pony_access_token";
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final UserService userService;

    public UserTokenFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = fetchAccessToken(request);
        if (token != null) {
            try {
                userService.authenticate(token);
            } catch (InvalidTokenException e) {
                logger.debug("Invalid token detected.");
            }
        }
        chain.doFilter(request, response);
    }
    
    @Nullable
    private String fetchAccessToken(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String token = fetchTokenFromHeaders(httpRequest);
        if (token != null) {
            return token;
        }
        if (!httpRequest.getServletPath().startsWith("/api/") && httpRequest.getMethod().equals("GET")) {
            token = fetchTokenFromCookies(httpRequest);
            if (token != null) {
                return token;
            }
        }
        return null;
    }
    
    @Nullable
    private String fetchTokenFromHeaders(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(TOKEN_HEADER_PREFIX)) {
            return header.substring(TOKEN_HEADER_PREFIX.length());
        } else {
            return null;
        }
    }
    
    @Nullable
    private String fetchTokenFromCookies(HttpServletRequest request) {
        return fetchCookie(request, TOKEN_COOKIE_NAME);
    }

    @Nullable
    private String fetchCookie(HttpServletRequest request, String name) {
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
