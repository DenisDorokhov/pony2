package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony2.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony2.api.user.service.exception.UserNotFoundException;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.security.token.TokenService;
import net.dorokhov.pony2.web.service.exception.NotAuthenticatedException;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserFacade {

    private static final int PAGE_SIZE = 30;

    private final UserService userService;
    private final UserContext userContext;
    private final TokenService tokenService;

    public UserFacade(
            UserService userService,
            UserContext userContext,
            TokenService tokenService
    ) {
        this.userService = userService;
        this.userContext = userContext;
        this.tokenService = tokenService;
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
    public UserPageDto getAllUsers(int pageIndex, int pageSize) {
        return UserPageDto.of(userService.getAll(PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)),
                Sort.by("name", "email"))));
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

    public OpenSubsonicApiKeyDto generateCurrentUserOpenSubsonicApiKey() {
        return new OpenSubsonicApiKeyDto().setValue(tokenService.generateOpenSubsonicApiKeyForUserId(userContext.getAuthenticatedUser().getId()));
    }
}
