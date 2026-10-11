package net.dorokhov.pony2.core.user.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Set;

import static net.dorokhov.pony2.test.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

public class UserRepositoryTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void shouldFindEarliestAdministratorRatherThanEarliestUser() {
        User ordinary = userRepository.save(user().setEmail("ordinary@example.com").setRoles(Set.of(User.Role.USER)));
        User firstAdmin = userRepository.save(user().setEmail("first@example.com").setRoles(Set.of(User.Role.USER, User.Role.ADMIN)));
        User laterAdmin = userRepository.save(user().setEmail("later@example.com").setRoles(Set.of(User.Role.ADMIN)));
        jdbcTemplate.update("UPDATE pony_user SET creation_date = ? WHERE id = ?", LocalDateTime.of(2019, 1, 1, 0, 0), ordinary.getId());
        jdbcTemplate.update("UPDATE pony_user SET creation_date = ? WHERE id = ?", LocalDateTime.of(2020, 1, 1, 0, 0), firstAdmin.getId());
        jdbcTemplate.update("UPDATE pony_user SET creation_date = ? WHERE id = ?", LocalDateTime.of(2021, 1, 1, 0, 0), laterAdmin.getId());
        assertThat(userRepository.findFirstByRolesContainingOrderByCreationDateAscIdAsc(User.Role.ADMIN)).contains(firstAdmin);
        userRepository.delete(firstAdmin);
        assertThat(userRepository.findFirstByRolesContainingOrderByCreationDateAscIdAsc(User.Role.ADMIN)).contains(laterAdmin);
    }

    @Test
    public void shouldSave() {
        User user = userRepository.save(user());
        assertThat(userRepository.findById(user.getId())).isNotEmpty();
    }
}
