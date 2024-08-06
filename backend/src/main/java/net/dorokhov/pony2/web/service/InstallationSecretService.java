package net.dorokhov.pony2.web.service;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import net.dorokhov.pony2.web.service.exception.SecretNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

@Component
public class InstallationSecretService {

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

    public String generateAndStoreInstallationSecret() throws IOException {
        logger.info("Generating new installation secret.");
        String randomKey = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomKeyService.generateRandomKey());
        Files.write(randomKey.getBytes(Charsets.UTF_8), installationSecretFile);
        return randomKey;
    }

    public String fetchInstallationSecret() throws SecretNotFoundException, IOException {
        if (!installationSecretFile.exists()) {
            throw new SecretNotFoundException();
        }
        return Files.toString(installationSecretFile, Charsets.UTF_8);
    }
}
