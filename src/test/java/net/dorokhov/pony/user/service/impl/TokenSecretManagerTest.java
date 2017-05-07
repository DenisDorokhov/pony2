package net.dorokhov.pony.user.service.impl;

import net.dorokhov.pony.user.service.exception.TokenSecretNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class TokenSecretManagerTest {
    
    private static final Resource FILE = new ClassPathResource("test.txt");

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void shouldGenerateAndStoreTokenSecret() throws Exception {
        TokenSecretManager tokenSecretManager = new TokenSecretManager(tempFolder.newFile());
        tokenSecretManager.generateAndStoreTokenSecret();
        assertThat(tokenSecretManager.getTokenSecret()).isNotNull();
    }

    @Test
    public void shouldGetTokenSecret() throws Exception {
        TokenSecretManager tokenSecretManager = new TokenSecretManager(FILE.getFile());
        assertThat(tokenSecretManager.getTokenSecret()).isEqualTo("test");
    }

    @Test
    public void shouldFailGettingNotExistingTokenSecret() throws Exception {
        TokenSecretManager tokenSecretManager = new TokenSecretManager(new File("notExistingFile"));
        assertThatThrownBy(tokenSecretManager::getTokenSecret).isInstanceOf(TokenSecretNotFoundException.class);
    }
}
