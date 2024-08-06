package net.dorokhov.pony2;

import jakarta.persistence.EntityManagerFactory;
import net.dorokhov.pony2.common.RethrowingLambdas;
import org.flywaydb.core.Flyway;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony2.common.RethrowingLambdas.rethrow;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "test"})
abstract public class IntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Value("${pony.artwork.path}")
    private File artworkFolder;
    
    private TransactionTemplate transactionTemplate;

    private final List<Class<?>> indexedClasses = fetchIndexedClasses();

    @Autowired
    private void initTransactionTemplate(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    protected TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    @BeforeEach
    void setUpIntegrationTest() {
        flyway.clean();
        purgeSearchIndexes();
        cleanFiles();
        flyway.migrate();
    }

    @AfterEach
    void tearDownIntegrationTest() {
        flyway.clean();
        purgeSearchIndexes();
        cleanFiles();
    }

    private void cleanFiles() {
        Optional.ofNullable(artworkFolder.listFiles())
                .map(Arrays::asList)
                .orElse(emptyList())
                .forEach(FileSystemUtils::deleteRecursively);
    }

    private void purgeSearchIndexes() {
        SearchMapping searchMapping = Search.mapping(entityManagerFactory);
        for (Class<?> indexedClass : indexedClasses) {
            searchMapping.scope(indexedClass).workspace().purge();
        }
    }

    private List<Class<?>> fetchIndexedClasses() {
        ClassPathScanningCandidateComponentProvider classProvider = new ClassPathScanningCandidateComponentProvider(false);
        classProvider.addIncludeFilter(new AnnotationTypeFilter(Indexed.class));
        String basePackage = ClassUtils.getPackageName(PonyApplication.class);
        return classProvider.findCandidateComponents(basePackage).stream()
                .map(rethrow((RethrowingLambdas.ThrowingFunction<BeanDefinition, Class<?>>)
                        beanDefinition -> Class.forName(beanDefinition.getBeanClassName())))
                .toList();
    }
}
