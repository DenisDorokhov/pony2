package net.dorokhov.pony.user;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.command.CurrentUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.domain.UserToken;
import net.dorokhov.pony.user.service.command.UserUpdateCommand;
import net.dorokhov.pony.user.service.exception.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {

    Optional<User> getById(Long id);
    Optional<User> getByEmail(String email);

    Page<User> getAll(Pageable pageable);

    User create(UserCreationCommand command) throws UserExistsException;
    User update(UserUpdateCommand command) throws UserNotFoundException, UserExistsException;

    void delete(Long id) throws UserNotFoundException, DeletingCurrentUserException;

    UserToken authenticate(String email, String password) throws InvalidCredentialsException;

    User authenticate(String token) throws InvalidTokenException;

    Optional<User> logout();

    Optional<User> getCurrentUser();
    User updateCurrentUser(CurrentUserUpdateCommand command) throws
            NotAuthenticatedException, InvalidPasswordException, 
            UserNotFoundException, UserExistsException;
}
