package net.dorokhov.pony.test;

import net.dorokhov.pony.PonyApplication;
import org.flywaydb.core.Flyway;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@Transactional
abstract public class IntegrationTest {
    
    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }
    
    protected TransactionTemplate transactionTemplate;
    
    @Before
    public void setUp() throws Exception {
        flyway.migrate();
    }

    @AfterTransaction
    public void tearDown() throws Exception {
        transactionTemplate.execute(status -> {
            try {
                return purgeAllSearchIndices();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        flyway.clean();
    }
    
    private List<Class> purgeAllSearchIndices() throws Exception {

        ClassPathScanningCandidateComponentProvider classProvider = new ClassPathScanningCandidateComponentProvider(false);
        classProvider.addIncludeFilter(new AnnotationTypeFilter(Indexed.class));
        
        String basePackage = ClassUtils.getPackageName(PonyApplication.class);

        FullTextEntityManager fullTextSession = Search.getFullTextEntityManager(entityManager);
        List<Class> purgedClasses = new ArrayList<>();
        for (BeanDefinition beanDefinition : classProvider.findCandidateComponents(basePackage)) {
            Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
            fullTextSession.purgeAll(clazz);
            purgedClasses.add(clazz);
        }
        
        return purgedClasses;
    }
}
