package net.dorokhov.pony.user.repository;

import net.dorokhov.pony.user.domain.User;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository extends PagingAndSortingRepository<User, Long> {
    User findByEmail(String email);
}
