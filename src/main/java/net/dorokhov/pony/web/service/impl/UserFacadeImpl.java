package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;
import net.dorokhov.pony.web.service.exception.NotAuthenticatedException;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import net.dorokhov.pony.web.domain.CurrentUserUpdateCommandDto;
import net.dorokhov.pony.web.domain.UserCreationCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.domain.UserUpdateCommandDto;
import net.dorokhov.pony.web.service.UserFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserFacadeImpl implements UserFacade {

    private final UserService userService;
    private final UserContext userContext;

    public UserFacadeImpl(UserService userService, UserContext userContext) {
        this.userService = userService;
        this.userContext = userContext;
    }

    @Override
    public UserDto getCurrentUser() throws NotAuthenticatedException {
        return UserDto.of(userContext.getAuthenticatedUser());
    }

    @Override
    @Transactional
    public UserDto updateCurrentUser(CurrentUserUpdateCommandDto command) throws InvalidPasswordException, DuplicateEmailException {
        User currentUser = userContext.getAuthenticatedUser();
        try {
            return UserDto.of(userService.update(command.convert(currentUser.getId())));
        } catch (UserNotFoundException e) {
            throw new IllegalStateException(String.format("Current user '%d' not found.", currentUser.getId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userService.getAll().stream()
                .map(UserDto::of)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) throws ObjectNotFoundException {
        User user = userService.getById(id);
        if (user == null) {
            throw new ObjectNotFoundException(User.class, id);
        }
        return UserDto.of(user);
    }

    @Override
    @Transactional
    public UserDto createUser(UserCreationCommandDto command) throws DuplicateEmailException {
        return UserDto.of(userService.create(command.convert()));
    }

    @Override
    @Transactional
    public UserDto updateUser(UserUpdateCommandDto command) throws ObjectNotFoundException, DuplicateEmailException {
        try {
            return UserDto.of(userService.update(command.convert()));
        } catch (UserNotFoundException e) {
            throw new ObjectNotFoundException(User.class, command.getId());
        }
    }
}
