package net.dorokhov.pony.web.service;

import net.dorokhov.pony.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;
import net.dorokhov.pony.web.domain.CurrentUserUpdateCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

public interface UserFacade {

    UserDto getCurrentUser();

    UserDto updateCurrentUser(@Valid @RequestBody CurrentUserUpdateCommandDto command)
            throws UserNotFoundException, InvalidPasswordException, DuplicateEmailException;
}
