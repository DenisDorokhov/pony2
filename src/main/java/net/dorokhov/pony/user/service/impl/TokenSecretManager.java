package net.dorokhov.pony.user.service.impl;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import net.dorokhov.pony.user.service.exception.TokenSecretNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class TokenSecretManager {

    private static final String CACHE_NAME = "pony.tokenSecret";
    private static final String CACHE_KEY = "'currentTokenSecret";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final File tokenSecretFile;

    public TokenSecretManager(@Value("${pony.tokenSecret.path}") File tokenSecretFile) {
        this.tokenSecretFile = tokenSecretFile;
    }

    @CachePut(cacheNames = CACHE_NAME, key = CACHE_KEY)
    public String generateAndStoreTokenSecret() throws IOException {
        logger.info("Generating new token secret.");
        String uuid = UUID.randomUUID().toString();
        Files.write(uuid, tokenSecretFile, Charsets.UTF_8);
        return uuid;
    }

    @Cacheable(cacheNames = CACHE_NAME, key = CACHE_KEY)
    public String getTokenSecret() throws TokenSecretNotFoundException {
        if (!tokenSecretFile.exists()) {
            throw new TokenSecretNotFoundException();
        }
        try {
            return Files.toString(tokenSecretFile, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
