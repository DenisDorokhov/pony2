package net.dorokhov.pony;

import net.dorokhov.pony.common.RethrowingLambdas;
import org.flywaydb.core.Flyway;
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

    private List<Class> indexedClasses;

    @Before
    public void setUp() throws Exception {
        flyway.migrate();
    }

    @After
    public void tearDown() throws Exception {
        cleanFiles();
        purgeIndexes();
        clearCache();
        flyway.clean();
    }

    private void cleanFiles() {
        Optional.ofNullable(artworkFolder.listFiles())
                .map(Arrays::asList)
                .orElse(emptyList())
                .forEach(FileSystemUtils::deleteRecursively);
    }

    @SuppressWarnings("unchecked")
    private void purgeIndexes() throws Exception {
        if (indexedClasses == null) {
            indexedClasses = fetchIndexedClasses();
        }
        FullTextEntityManager fullTextSession = Search.getFullTextEntityManager(entityManager);
        indexedClasses.forEach(fullTextSession::purgeAll);
        fullTextSession.flushToIndexes();
    }

    private void clearCache() {
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .forEach(Cache::clear);
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
