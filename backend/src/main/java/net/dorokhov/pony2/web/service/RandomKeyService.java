package net.dorokhov.pony2.web.service;

import com.google.common.io.Files;
import net.dorokhov.pony2.web.service.exception.SecretNotFoundException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

@Component
public class RandomKeyService {
    
    public byte[] generateRandomKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomKey = new byte[16];
        secureRandom.nextBytes(randomKey);
        return randomKey;
    }

    public byte[] generateAndStoreRandomKey(File file) throws IOException {
        byte[] randomKey = generateRandomKey();
        Files.write(randomKey, file);
        return randomKey;
    }
    
    public byte[] fetchStoredKey(File file) throws SecretNotFoundException, IOException {
        if (!file.exists()) {
            throw new SecretNotFoundException();
        }
        return Files.toByteArray(file);
    }
}
