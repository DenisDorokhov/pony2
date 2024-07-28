package net.dorokhov.pony3.web.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class BruteForceProtector {

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    private final LoadingCache<String, Integer> loginAttemptCounter = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<>() {
                @Override
                public @Nonnull Integer load(@Nonnull String key) {
                    return 0;
                }
            });

    public synchronized void onFailedLogin(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        int attempts;
        try {
            attempts = loginAttemptCounter.get(clientIp);
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not fetch number of login attempts for IP '" + clientIp + "'.", e);
        }
        if (attempts < MAX_LOGIN_ATTEMPTS) {
            loginAttemptCounter.put(clientIp, attempts + 1);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null) {
            return xForwardedForHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    public synchronized boolean shouldBlockLogin(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        int attempts;
        try {
            attempts = loginAttemptCounter.get(clientIp);
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not fetch number of login attempts for IP '" + clientIp + "'.", e);
        }
        return attempts >= MAX_LOGIN_ATTEMPTS;
    }
}
