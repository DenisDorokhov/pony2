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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ClassUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"dev", "test"})
abstract public class IntegrationTest {
    
    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManager entityManager;
    
    private List<Class> indexedClasses;
    
    @Before
    public void setUp() throws Exception {
        flyway.migrate();
    }

    @After
    public void tearDown() throws Exception {
        purgeIndexes();
        flyway.clean();
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
