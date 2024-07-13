package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.web.service.exception.SecretNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RandomKeyServiceTest {

    private static final Resource SECRET_RESOURCE = new ClassPathResource("test.txt");

    private RandomKeyService randomKeyService = new RandomKeyService();

    @TempDir
    public Path tempFolder;

    @Test
    public void shouldGenerateRandomKey() {
        assertThat(randomKeyService.generateRandomKey()).isNotNull();
    }

    @Test
    public void shouldGenerateAndStoreRandomKey() throws IOException, SecretNotFoundException {

        File file = Files.createFile(tempFolder.resolve("file")).toFile();

        assertThat(randomKeyService.generateAndStoreRandomKey(file)).isNotNull();
        assertThat(randomKeyService.fetchStoredKey(file)).isNotNull();
    }

    @Test
    public void shouldFetchStoredKey() throws IOException, SecretNotFoundException {
        assertThat(randomKeyService.fetchStoredKey(SECRET_RESOURCE.getFile()))
                .isEqualTo(new byte[]{0x74, 0x65, 0x73, 0x74});
    }

    @Test
    public void shouldFailFetchingStoredKeyIfFileNotFound() {

        File file = new File("notExistingFile");

        assertThatThrownBy(() -> randomKeyService.fetchStoredKey(file)).isInstanceOf(SecretNotFoundException.class);
    }
}