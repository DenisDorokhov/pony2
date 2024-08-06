package net.dorokhov.pony2.web.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import net.dorokhov.pony2.api.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class BruteForceProtector {

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
    private final int maxLoginAttempts;

    public BruteForceProtector(
            LogService logService,
            @Value("${pony.maxLoginAttempts}") int maxLoginAttempts
    ) {
        this.logService = logService;
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public synchronized void onSuccessfulLoginAttempt(HttpServletRequest request, String username) {
        if (maxLoginAttempts < 0) {
            return;
        }
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
        if (maxLoginAttempts < 0) {
            return;
        }
        String clientIp = resolveIpAddress(request);
        int attempts;
        try {
            attempts = loginAttemptCounter.get(clientIp);
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not fetch number of login attempts for IP '" + clientIp + "' after failed login attempt.", e);
        }
        if (attempts < maxLoginAttempts) {
            attempts++;
            loginAttemptCounter.put(clientIp, attempts);
            logService.warn(logger, "Failed login attempt for '{}' from '{}' with {} total attempts.",
                    username, clientIp, attempts);
        } else {
            logService.error(logger, "Blocked login attempt for '{}' from '{}' with {} total attempts.",
                    username, clientIp, attempts);
        }
    }

    public synchronized boolean shouldBlockLoginAttempt(HttpServletRequest request) {
        if (maxLoginAttempts < 0) {
            return false;
        }
        String clientIp = resolveIpAddress(request);
        int attempts;
        try {
            attempts = loginAttemptCounter.get(clientIp);
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not fetch number of login attempts for IP '" + clientIp + "' to check for blocked login attempt.", e);
        }
        return attempts >= maxLoginAttempts;
    }

    public synchronized void clearLoginAttempts() {
        loginAttemptCounter.invalidateAll();
    }
}
