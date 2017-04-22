package net.dorokhov.pony.user;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import net.dorokhov.pony.user.exception.TokenSecretNotFoundException;
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
public class TokenSecretManagerImpl implements TokenSecretManager {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final File tokenSecretFile;
    
    final AtomicReference<String> tokenSecret = new AtomicReference<>();

    public TokenSecretManagerImpl(@Value("${pony.tokenSecret.path}") File tokenSecretFile) {
        this.tokenSecretFile = tokenSecretFile;
    }
    
    @PostConstruct
    void init() {
        try {
            getTokenSecret();
        } catch (TokenSecretNotFoundException e) {
            log.debug("Token secret not found.");
        }
    }

    @Override
    public String generateAndStoreTokenSecret() throws IOException {
        String uuid = UUID.randomUUID().toString();
        Files.write(uuid, tokenSecretFile, Charsets.UTF_8);
        tokenSecret.set(uuid);
        return uuid;
    }

    @Override
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
