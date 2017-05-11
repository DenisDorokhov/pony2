package net.dorokhov.pony.user.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.user.domain.User;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRepositoryTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    public void shouldSave() throws Exception {
        User user = userRepository.save(userBuilder().build());
        assertThat(userRepository.findOne(user.getId())).isNotNull();
    }

    @Test
    public void shouldCacheUser() throws Exception {
        
        Statistics statistics = getStatistics();
        statistics.setStatisticsEnabled(true);

        User user = userRepository.save(userBuilder().name("oldName").build());
        statistics.clear();
        user = userRepository.findOne(user.getId());
        assertThat(statistics.getSecondLevelCacheMissCount()).isEqualTo(1);
        assertThat(statistics.getSecondLevelCacheHitCount()).isEqualTo(0);
        assertThat(statistics.getSecondLevelCachePutCount()).isEqualTo(1);
        
        statistics.clear();
        user = userRepository.findOne(user.getId());
        assertThat(statistics.getSecondLevelCacheMissCount()).isEqualTo(0);
        assertThat(statistics.getSecondLevelCacheHitCount()).isEqualTo(1);
        assertThat(statistics.getSecondLevelCachePutCount()).isEqualTo(0);

        user = userRepository.save(User.builder(user).name("newName").build());
        statistics.clear();
        user = userRepository.findOne(user.getId());
        assertThat(user.getName()).isEqualTo("newName");
        assertThat(statistics.getSecondLevelCacheMissCount()).isEqualTo(1);
        assertThat(statistics.getSecondLevelCacheHitCount()).isEqualTo(0);
        assertThat(statistics.getSecondLevelCachePutCount()).isEqualTo(1);
    }
    
    private User.Builder userBuilder() {
        return User.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .addRoles(User.Role.USER, User.Role.ADMIN);
    }
    
    private Statistics getStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }
}
