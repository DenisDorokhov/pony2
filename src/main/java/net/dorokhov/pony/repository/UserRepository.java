package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.User;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository extends PagingAndSortingRepository<User, Long> {

    User findByEmail(String email);

    User findByEmailAndPassword(String email, String password);
}
