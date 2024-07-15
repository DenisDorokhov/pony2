package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.api.user.service.UserService;
import net.dorokhov.pony3.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony3.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony3.api.user.service.exception.UserNotFoundException;
import net.dorokhov.pony3.web.dto.CurrentUserUpdateCommandDto;
import net.dorokhov.pony3.web.dto.UserCreationCommandDto;
import net.dorokhov.pony3.web.dto.UserDto;
import net.dorokhov.pony3.web.dto.UserUpdateCommandDto;
import net.dorokhov.pony3.web.service.exception.NotAuthenticatedException;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserFacade {

    private final UserService userService;
    private final UserContext userContext;

    public UserFacade(UserService userService, UserContext userContext) {
        this.userService = userService;
        this.userContext = userContext;
    }

    public UserDto getCurrentUser() throws NotAuthenticatedException {
        return UserDto.of(userContext.getAuthenticatedUser());
    }

    @Transactional
    public UserDto updateCurrentUser(CurrentUserUpdateCommandDto command) throws InvalidPasswordException, DuplicateEmailException {
        User currentUser = userContext.getAuthenticatedUser();
        try {
            return UserDto.of(userService.update(command.convert(currentUser.getId())));
        } catch (UserNotFoundException e) {
            throw new IllegalStateException(String.format("Current user '%s' not found.", currentUser.getId()));
        }
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userService.getAll().stream()
                .map(UserDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(String id) throws ObjectNotFoundException {
        User user = userService.getById(id).orElse(null);
        if (user == null) {
            throw new ObjectNotFoundException(User.class, id);
        }
        return UserDto.of(user);
    }

    @Transactional
    public UserDto createUser(UserCreationCommandDto command) throws DuplicateEmailException {
        return UserDto.of(userService.create(command.convert()));
    }

    @Transactional
    public UserDto updateUser(UserUpdateCommandDto command) throws ObjectNotFoundException, DuplicateEmailException {
        try {
            return UserDto.of(userService.update(command.convert()));
        } catch (UserNotFoundException e) {
            throw new ObjectNotFoundException(User.class, command.getId());
        }
    }

    @Transactional
    public UserDto deleteUserById(String id) throws ObjectNotFoundException {
        User user = userService.getById(id).orElse(null);
        try {
            userService.delete(id);
        } catch (UserNotFoundException e) {
            throw new ObjectNotFoundException(User.class, id);
        }
        //noinspection ConstantConditions
        return UserDto.of(user);
    }
}
