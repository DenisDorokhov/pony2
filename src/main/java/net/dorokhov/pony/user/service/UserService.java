package net.dorokhov.pony.user.service;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;

import javax.annotation.Nullable;
import java.util.List;

public interface UserService {

    @Nullable 
    User getById(Long id);
    @Nullable
    User getByEmail(String email);

    List<User> getAll();

    User create(UserCreationCommand command) throws DuplicateEmailException;
    User update(UnsafeUserUpdateCommand command) throws UserNotFoundException, DuplicateEmailException;
    User update(SafeUserUpdateCommand command) throws InvalidPasswordException, UserNotFoundException, DuplicateEmailException;

    void delete(Long id) throws UserNotFoundException;
}
