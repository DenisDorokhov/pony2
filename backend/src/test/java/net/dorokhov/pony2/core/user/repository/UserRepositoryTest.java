package net.dorokhov.pony2.core.user.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.dorokhov.pony2.test.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

public class UserRepositoryTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void shouldSave() {
        User user = userRepository.save(user());
        assertThat(userRepository.findById(user.getId())).isNotEmpty();
    }
}
