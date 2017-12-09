package net.dorokhov.pony.web.service;

import java.io.File;
import java.io.IOException;

import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstallationSecretManager {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SecretManager secretManager;
    private final File installationSecretFile;

    public InstallationSecretManager(SecretManager secretManager, 
                                     @Value("${pony.installationSecret.path}") File installationSecretFile) {
        this.secretManager = secretManager;
        this.installationSecretFile = installationSecretFile;
    }
    
    public String generateAndStoreInstallationSecret() throws IOException {
        logger.info("Generating new installation secret.");
        return secretManager.generateAndStoreSecret(installationSecretFile);
    }

    public String fetchInstallationSecret() throws SecretNotFoundException, IOException {
        return secretManager.fetchSecret(installationSecretFile);
    }
}
