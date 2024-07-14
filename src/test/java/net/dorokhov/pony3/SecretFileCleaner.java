package net.dorokhov.pony3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.File;

@Component
@Profile("test")
@SuppressWarnings("unused")
public class SecretFileCleaner {

    private final File installationSecretFile;
    private final File accessTokenKeyFile;
    private final File staticTokenKeyFile;

    public SecretFileCleaner(
            @Value("${pony.installationSecret.path}") File installationSecretFile,
            @Value("${pony.accessTokenKey.path}") File accessTokenKeyFile,
            @Value("${pony.staticTokenKey.path}") File staticTokenKeyFile
    ) {
        this.accessTokenKeyFile = accessTokenKeyFile;
        this.installationSecretFile = installationSecretFile;
        this.staticTokenKeyFile = staticTokenKeyFile;
    }

    @PreDestroy
    public void cleanSecretFiles() {
        accessTokenKeyFile.delete();
        staticTokenKeyFile.delete();
        installationSecretFile.delete();
    }
}
