package net.dorokhov.pony3.web.service;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
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
import java.util.Base64;

@Component
@CacheConfig(cacheNames = "pony.installationSecret")
public class InstallationSecretService {

    private static final String CACHE_KEY = "'installationSecret'";
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RandomKeyService randomKeyService;
    private final File installationSecretFile;

    public InstallationSecretService(
            RandomKeyService randomKeyService,
            @Value("${pony.installationSecret.path}") File installationSecretFile
    ) {
        this.randomKeyService = randomKeyService;
        this.installationSecretFile = installationSecretFile;
    }

    @CachePut(key = CACHE_KEY)
    public String generateAndStoreInstallationSecret() throws IOException {
        logger.info("Generating new installation secret.");
        String randomKey = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomKeyService.generateRandomKey());
        Files.write(randomKey.getBytes(Charsets.UTF_8), installationSecretFile);
        return randomKey;
    }

    @Cacheable(key = CACHE_KEY)
    public String fetchInstallationSecret() throws SecretNotFoundException, IOException {
        if (!installationSecretFile.exists()) {
            throw new SecretNotFoundException();
        }
        return Files.toString(installationSecretFile, Charsets.UTF_8);
    }
}
