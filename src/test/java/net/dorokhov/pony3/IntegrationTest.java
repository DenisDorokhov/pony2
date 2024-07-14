package net.dorokhov.pony3;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "test"})
abstract public class IntegrationTest {

    @Autowired
    private Flyway flyway;

    @Value("${pony.artwork.path}")
    private File artworkFolder;
    
    private TransactionTemplate transactionTemplate;

    @Autowired
    private void initTransactionTemplate(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    @BeforeEach
    void setUpIntegrationTest() {
        flyway.clean();
        cleanFiles();
        flyway.migrate();
    }

    @AfterEach
    void tearDownIntegrationTest() {
        flyway.clean();
        cleanFiles();
    }

    protected TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    private void cleanFiles() {
        Optional.ofNullable(artworkFolder.listFiles())
                .map(Arrays::asList)
                .orElse(emptyList())
                .forEach(FileSystemUtils::deleteRecursively);
    }
}
