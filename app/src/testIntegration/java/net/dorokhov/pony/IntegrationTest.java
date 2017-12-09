package net.dorokhov.pony;

import net.dorokhov.pony.common.RethrowingLambdas;
import org.flywaydb.core.Flyway;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileSystemUtils;

import javax.persistence.EntityManager;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "test"})
abstract public class IntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CacheManager cacheManager;

    @Value("${pony.artwork.path}")
    private File artworkFolder;
    
    private TransactionTemplate transactionTemplate;

    private List<Class> indexedClasses;
    
    @Autowired
    private void initTransactionTemplate(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    @Before
    public void setUp() throws Exception {
        flyway.migrate();
    }

    @After
    public void tearDown() throws Exception {
        cleanFiles();
        purgeSearchIndexes();
        clearCache();
        flyway.clean();
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

    @SuppressWarnings("unchecked")
    private void purgeSearchIndexes() throws Exception {
        if (indexedClasses == null) {
            indexedClasses = fetchIndexedClasses();
        }
        FullTextEntityManager fullTextSession = Search.getFullTextEntityManager(entityManager);
        indexedClasses.forEach(fullTextSession::purgeAll);
        fullTextSession.flushToIndexes();
    }

    private void clearCache() {
        // Clear Spring cache.
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .forEach(Cache::clear);
        // Clear Hibernate second level cache.
        transactionTemplate.execute(status -> {
            SessionFactory sessionFactory = entityManager.unwrap(Session.class).getSessionFactory();
            sessionFactory.getCache().evictAllRegions();
            return null;
        });
    }

    private List<Class> fetchIndexedClasses() throws Exception {
        ClassPathScanningCandidateComponentProvider classProvider = new ClassPathScanningCandidateComponentProvider(false);
        classProvider.addIncludeFilter(new AnnotationTypeFilter(Indexed.class));
        String basePackage = ClassUtils.getPackageName(PonyApplication.class);
        return classProvider.findCandidateComponents(basePackage).stream()
                .map(rethrow((RethrowingLambdas.ThrowingFunction<BeanDefinition, Class>)
                        beanDefinition -> Class.forName(beanDefinition.getBeanClassName())))
                .collect(Collectors.toList());
    }
}
