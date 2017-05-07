package net.dorokhov.pony.user.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.user.domain.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRepositoryTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void shouldSave() throws Exception {
        User user = userRepository.save(User.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .addRoles(User.Role.USER, User.Role.ADMIN)
                .build());
        assertThat(userRepository.findOne(user.getId())).isNotNull();
    }
}
