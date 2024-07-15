package net.dorokhov.pony3.api.user.service;

import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.api.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony3.api.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony3.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony3.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony3.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony3.api.user.service.exception.UserNotFoundException;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<User> getById(String id);
    Optional<User> getByEmail(String email);

    List<User> getAll();
    
    boolean checkUserPassword(String id, String password) throws UserNotFoundException;

    User create(UserCreationCommand command) throws DuplicateEmailException;
    User update(UnsafeUserUpdateCommand command) throws UserNotFoundException, DuplicateEmailException;
    User update(SafeUserUpdateCommand command) throws InvalidPasswordException, UserNotFoundException, DuplicateEmailException;

    void delete(String id) throws UserNotFoundException;
}