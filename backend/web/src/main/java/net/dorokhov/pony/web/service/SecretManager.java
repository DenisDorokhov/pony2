package net.dorokhov.pony.web.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class SecretManager {

    public String generateAndStoreSecret(File file) throws IOException {
        String uuid = UUID.randomUUID().toString();
        Files.write(uuid, file, Charsets.UTF_8);
        return uuid;
    }
    
    public String fetchSecret(File file) throws SecretNotFoundException, IOException {
        if (!file.exists()) {
            throw new SecretNotFoundException();
        }
        try {
            return Files.toString(file, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read token secret file.", e);
        }
    }
}
