package net.dorokhov.pony.user.service.impl;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import net.dorokhov.pony.user.service.exception.TokenSecretNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TokenSecretManager {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final File tokenSecretFile;
    
    final AtomicReference<String> tokenSecret = new AtomicReference<>();

    public TokenSecretManager(@Value("${pony.tokenSecret.path}") File tokenSecretFile) {
        this.tokenSecretFile = tokenSecretFile;
    }
    
    @PostConstruct
    void init() {
        try {
            getTokenSecret();
        } catch (TokenSecretNotFoundException e) {
            logger.debug("Token secret not found.");
        }
    }

    public String generateAndStoreTokenSecret() throws IOException {
        logger.info("Generating new token secret.");
        String uuid = UUID.randomUUID().toString();
        Files.write(uuid, tokenSecretFile, Charsets.UTF_8);
        tokenSecret.set(uuid);
        return uuid;
    }

    public String getTokenSecret() throws TokenSecretNotFoundException {
        return tokenSecret.updateAndGet(secret -> {
            if (secret == null) {
                if (!tokenSecretFile.exists()) {
                    throw new TokenSecretNotFoundException();
                }
                try {
                    return Files.toString(tokenSecretFile, Charsets.UTF_8);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                return secret;
            }
        });
    }
}
