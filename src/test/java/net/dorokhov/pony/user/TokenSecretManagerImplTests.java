package net.dorokhov.pony.user;

import net.dorokhov.pony.user.exception.TokenSecretNotFoundException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class TokenSecretManagerImplTests {
    
    private static final Resource FILE = new ClassPathResource("test.txt");

    private File writeToFile;

    @After
    public void tearDown() throws Exception {
        if (writeToFile != null) {
            if (!writeToFile.delete()) {
                throw new RuntimeException("Could not delete temporary file.");
            }
            writeToFile = null;
        }
    }

    @Test
    public void initializeTokenSecret() throws Exception {
        TokenSecretManagerImpl tokenSecretManager = new TokenSecretManagerImpl(FILE.getFile());
        tokenSecretManager.init();
        assertThat(tokenSecretManager.tokenSecret.get()).isEqualTo("test");
    }
    
    @Test
    public void acceptNoTokenSecretWhenInitializing() throws Exception {
        TokenSecretManagerImpl tokenSecretManager = new TokenSecretManagerImpl(new File("notExistingFile"));
        tokenSecretManager.init();
    }

    @Test
    public void generateAndStoreTokenSecret() throws Exception {
        writeToFile = File.createTempFile(getClass().getSimpleName(), "txt");
        TokenSecretManagerImpl tokenSecretManager = new TokenSecretManagerImpl(writeToFile);
        tokenSecretManager.generateAndStoreTokenSecret();
        assertThat(tokenSecretManager.tokenSecret.get()).isNotNull();
    }

    @Test
    public void getTokenSecret() throws Exception {
        TokenSecretManagerImpl tokenSecretManager = new TokenSecretManagerImpl(FILE.getFile());
        assertThat(tokenSecretManager.getTokenSecret()).isEqualTo("test");
        assertThat(tokenSecretManager.tokenSecret.get()).isEqualTo("test");
    }

    @Test
    public void getNotExistingTokenSecret() throws Exception {
        TokenSecretManagerImpl tokenSecretManager = new TokenSecretManagerImpl(new File("notExistingFile"));
        assertThatThrownBy(tokenSecretManager::getTokenSecret).isInstanceOf(TokenSecretNotFoundException.class);
    }
}
