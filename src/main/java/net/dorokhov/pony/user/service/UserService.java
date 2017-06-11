package net.dorokhov.pony.user.service;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserExistsException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nullable;

public interface UserService {

    @Nullable 
    User getById(Long id);
    @Nullable
    User getByEmail(String email);

    Page<User> getAll(Pageable pageable);

    User create(UserCreationCommand command) throws UserExistsException;
    User update(UnsafeUserUpdateCommand command) throws UserNotFoundException, UserExistsException;
    User update(SafeUserUpdateCommand command) throws InvalidPasswordException, UserNotFoundException, UserExistsException;

    void delete(Long id) throws UserNotFoundException;
}
