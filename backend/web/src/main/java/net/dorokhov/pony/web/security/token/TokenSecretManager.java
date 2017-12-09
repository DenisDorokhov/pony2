package net.dorokhov.pony.web.security.token;

import net.dorokhov.pony.web.service.SecretManager;
import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
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
@CacheConfig(cacheNames = "pony.tokenSecret")
public class TokenSecretManager {

    private static final String CACHE_KEY = "'currentTokenSecret'";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SecretManager secretManager;
    private final File tokenSecretFile;

    public TokenSecretManager(SecretManager secretManager, 
                              @Value("${pony.tokenSecret.path}") File tokenSecretFile) {
        this.secretManager = secretManager;
        this.tokenSecretFile = tokenSecretFile;
    }

    @CachePut(key = CACHE_KEY)
    public String generateAndStoreTokenSecret() throws IOException {
        logger.info("Generating new token secret.");
        return secretManager.generateAndStoreSecret(tokenSecretFile);
    }

    @Cacheable(key = CACHE_KEY)
    public String fetchTokenSecret() throws SecretNotFoundException, IOException {
        return secretManager.fetchSecret(tokenSecretFile);
    }
}
