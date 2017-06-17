package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;
import net.dorokhov.pony.web.domain.CurrentUserUpdateCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.service.UserFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserFacadeImpl implements UserFacade {

    private final UserService userService;
    private final UserContext userContext;

    public UserFacadeImpl(UserService userService, UserContext userContext) {
        this.userService = userService;
        this.userContext = userContext;
    }

    @Override
    public UserDto getCurrentUser() {
        return UserDto.of(userContext.getAuthenticatedUser());
    }

    @Override
    @Transactional
    public UserDto updateCurrentUser(CurrentUserUpdateCommandDto command)
            throws UserNotFoundException, InvalidPasswordException, DuplicateEmailException {
        User currentUser = userContext.getAuthenticatedUser();
        return UserDto.of(userService.update(command.convert(currentUser.getId())));
    }
}
