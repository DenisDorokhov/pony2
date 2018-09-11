package net.dorokhov.pony.core.user.repository;

import net.dorokhov.pony.api.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    User findByEmail(String email);
}
