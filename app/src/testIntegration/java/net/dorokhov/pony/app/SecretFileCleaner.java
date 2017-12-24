package net.dorokhov.pony.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.File;

@Component
@Profile("test")
@SuppressWarnings("unused")
public class SecretFileCleaner {
    
    private final File tokenSecretFile;
    private final File installationSecretFile;

    public SecretFileCleaner(@Value("${pony.tokenSecret.path}") File tokenSecretFile,
                             @Value("${pony.installationSecret.path}") File installationSecretFile) {
        this.tokenSecretFile = tokenSecretFile;
        this.installationSecretFile = installationSecretFile;
    }
    
    @PreDestroy
    public void cleanSecretFiles() {
        tokenSecretFile.delete();
        installationSecretFile.delete();
    }
}
