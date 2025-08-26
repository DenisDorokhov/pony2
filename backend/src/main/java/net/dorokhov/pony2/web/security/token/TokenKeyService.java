package net.dorokhov.pony2.web.security.token;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dorokhov.pony2.web.service.RandomKeyService;
import net.dorokhov.pony2.web.service.exception.SecretNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Component
public class TokenKeyService {

    private static final String CACHE_KEY_ACCESS_TOKEN = "accessToken";
    private static final String CACHE_KEY_STATIC_TOKEN = "staticToken";
    private static final String CACHE_KEY_OPEN_SUBSONIC = "openSubsonic";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Cache<String, byte[]> cache = CacheBuilder.newBuilder()
            .maximumSize(2)
            .build();

    private final RandomKeyService randomKeyService;
    private final File accessTokenKeyFile;
    private final File staticTokenKeyFile;
    private final File openSubsonicKeyFile;

    public TokenKeyService(
            RandomKeyService randomKeyService,
            @Value("${pony.accessTokenKey.path}") File accessTokenKeyFile,
            @Value("${pony.staticTokenKey.path}") File staticTokenKeyFile,
            @Value("${pony.openSubsonicKey.path}") File openSubsonicKeyFile
    ) {
        this.randomKeyService = randomKeyService;
        this.accessTokenKeyFile = accessTokenKeyFile;
        this.staticTokenKeyFile = staticTokenKeyFile;
        this.openSubsonicKeyFile = openSubsonicKeyFile;
    }

    public byte[] generateAndStoreAccessTokenKey() {
        logger.info("Generating new access token secret.");
        byte[] result;
        try {
            result = randomKeyService.generateAndStoreRandomKey(accessTokenKeyFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not generate access token secret.", e);
        }
        cache.put(CACHE_KEY_ACCESS_TOKEN, result);
        return result;
    }

    public byte[] fetchAccessTokenKey() throws SecretNotFoundException {
        try {
            return cache.get(CACHE_KEY_ACCESS_TOKEN, () -> randomKeyService.fetchStoredKey(accessTokenKeyFile));
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SecretNotFoundException) {
                throw (SecretNotFoundException) e.getCause();
            } else {
                throw new RuntimeException("Could not fetch access token secret.", e);
            }
        }
    }

    public byte[] generateAndStoreStaticTokenKey() {
        logger.info("Generating new static token secret.");
        byte[] result;
        try {
            result = randomKeyService.generateAndStoreRandomKey(staticTokenKeyFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not generate static token secret.", e);
        }
        cache.put(CACHE_KEY_STATIC_TOKEN, result);
        return result;
    }

    public byte[] fetchStaticTokenKey() throws SecretNotFoundException {
        try {
            return cache.get(CACHE_KEY_STATIC_TOKEN, () -> randomKeyService.fetchStoredKey(staticTokenKeyFile));
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SecretNotFoundException) {
                throw (SecretNotFoundException) e.getCause();
            } else {
                throw new RuntimeException("Could not fetch static token secret.", e);
            }
        }
    }

    public byte[] generateAndStoreOpenSubsonicKey() {
        logger.info("Generating new OpenSubsonic secret.");
        byte[] result;
        try {
            result = randomKeyService.generateAndStoreRandomKey(openSubsonicKeyFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not generate OpenSubsonic secret.", e);
        }
        cache.put(CACHE_KEY_OPEN_SUBSONIC, result);
        return result;
    }

    public byte[] fetchOpenSubsonicKey() throws SecretNotFoundException {
        try {
            return cache.get(CACHE_KEY_OPEN_SUBSONIC, () -> randomKeyService.fetchStoredKey(openSubsonicKeyFile));
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SecretNotFoundException) {
                throw (SecretNotFoundException) e.getCause();
            } else {
                throw new RuntimeException("Could not fetch OpenSubsonic secret.", e);
            }
        }
    }

    public void clearCache() {
        cache.invalidateAll();
    }
}
