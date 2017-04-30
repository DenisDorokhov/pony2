package net.dorokhov.pony.user;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.command.CurrentUserUpdateDraft;
import net.dorokhov.pony.user.service.command.UserCreationDraft;
import net.dorokhov.pony.user.domain.UserToken;
import net.dorokhov.pony.user.service.command.UserUpdateDraft;
import net.dorokhov.pony.user.service.exception.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {

    Optional<User> getById(Long id);
    Optional<User> getByEmail(String email);

    Page<User> getAll(Pageable pageable);

    User create(UserCreationDraft draft) throws UserExistsException;
    User update(UserUpdateDraft draft) throws UserNotFoundException, UserExistsException;

    void delete(Long id) throws UserNotFoundException, DeletingCurrentUserException;

    UserToken authenticate(String email, String password) throws InvalidCredentialsException;

    User authenticate(String token) throws InvalidTokenException;

    Optional<User> logout();

    Optional<User> getCurrentUser();
    User updateCurrentUser(CurrentUserUpdateDraft draft) throws
            NotAuthenticatedException, InvalidPasswordException, 
            UserNotFoundException, UserExistsException;
}
