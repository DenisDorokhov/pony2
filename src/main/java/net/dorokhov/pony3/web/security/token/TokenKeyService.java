package net.dorokhov.pony3.web.security.token;

import net.dorokhov.pony3.web.service.RandomKeyService;
import net.dorokhov.pony3.web.service.exception.SecretNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@CacheConfig(cacheNames = "pony.tokenKey")
public class TokenKeyService {

    private static final String ACCESS_TOKEN_CACHE_KEY = "'accessTokenKey'";
    private static final String STATIC_TOKEN_CACHE_KEY = "'staticTokenKey'";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RandomKeyService randomKeyService;
    private final File accessTokenKeyFile;
    private final File staticTokenKeyFile;

    public TokenKeyService(
            RandomKeyService randomKeyService,
            @Value("${pony.accessTokenKey.path}") File accessTokenKeyFile,
            @Value("${pony.staticTokenKey.path}") File staticTokenKeyFile
    ) {
        this.randomKeyService = randomKeyService;
        this.accessTokenKeyFile = accessTokenKeyFile;
        this.staticTokenKeyFile = staticTokenKeyFile;
    }

    @CachePut(key = ACCESS_TOKEN_CACHE_KEY)
    public byte[] generateAndStoreAccessTokenKey() throws IOException {
        logger.info("Generating new token secret.");
        return randomKeyService.generateAndStoreRandomKey(accessTokenKeyFile);
    }

    @Cacheable(key = ACCESS_TOKEN_CACHE_KEY)
    public byte[] fetchAccessTokenKey() throws SecretNotFoundException, IOException {
        return randomKeyService.fetchStoredKey(accessTokenKeyFile);
    }

    @CachePut(key = STATIC_TOKEN_CACHE_KEY)
    public byte[] generateAndStoreStaticTokenKey() throws IOException {
        logger.info("Generating new token secret.");
        return randomKeyService.generateAndStoreRandomKey(staticTokenKeyFile);
    }

    @Cacheable(key = STATIC_TOKEN_CACHE_KEY)
    public byte[] fetchStaticTokenKey() throws SecretNotFoundException, IOException {
        return randomKeyService.fetchStoredKey(staticTokenKeyFile);
    }
}
