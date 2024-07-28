package net.dorokhov.pony3.web.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import net.dorokhov.pony3.api.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class BruteForceProtector {

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LoadingCache<String, Integer> loginAttemptCounter = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<>() {
                @Override
                public @Nonnull Integer load(@Nonnull String key) {
                    return 0;
                }
            });

    private final LogService logService;

    public BruteForceProtector(LogService logService) {
        this.logService = logService;
    }

    public synchronized void onSuccessfulLoginAttempt(HttpServletRequest request, String username) {
        String clientIp = resolveIpAddress(request);
        loginAttemptCounter.put(clientIp, 0);
        logService.info(logger, "Successful login attempt for '{}' from '{}'.",
                username, clientIp);
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null) {
            return xForwardedForHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    public synchronized void onFailedLoginAttempt(HttpServletRequest request, String username) {
        String clientIp = resolveIpAddress(request);
        int attempts;
        try {
            attempts = loginAttemptCounter.get(clientIp);
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not fetch number of login attempts for IP '" + clientIp + "'.", e);
        }
        if (attempts < MAX_LOGIN_ATTEMPTS) {
            attempts++;
            loginAttemptCounter.put(clientIp, attempts);
            logService.info(logger, "Failed login attempt for '{}' from '{}' with {} total attempts.",
                    username, clientIp, attempts);
        } else {
            logService.info(logger, "Blocked login attempt for '{}' from '{}' with {} total attempts.",
                    username, clientIp, attempts);
        }
    }

    public synchronized boolean shouldBlockLoginAttempt(HttpServletRequest request) {
        String clientIp = resolveIpAddress(request);
        int attempts;
        try {
            attempts = loginAttemptCounter.get(clientIp);
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not fetch number of login attempts for IP '" + clientIp + "'.", e);
        }
        return attempts >= MAX_LOGIN_ATTEMPTS;
    }
}
