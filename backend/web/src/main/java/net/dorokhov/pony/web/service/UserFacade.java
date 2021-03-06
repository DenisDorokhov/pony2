package net.dorokhov.pony.web.service;

import net.dorokhov.pony.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.web.domain.CurrentUserUpdateCommandDto;
import net.dorokhov.pony.web.domain.UserCreationCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.domain.UserUpdateCommandDto;
import net.dorokhov.pony.web.service.exception.NotAuthenticatedException;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;

import java.util.List;

public interface UserFacade {

    UserDto getCurrentUser() throws NotAuthenticatedException;
    UserDto updateCurrentUser(CurrentUserUpdateCommandDto command) throws InvalidPasswordException, DuplicateEmailException;
    
    List<UserDto> getAllUsers();
    
    UserDto getUserById(String id) throws ObjectNotFoundException;
    
    UserDto createUser(UserCreationCommandDto command) throws DuplicateEmailException;
    UserDto updateUser(UserUpdateCommandDto command) throws ObjectNotFoundException, DuplicateEmailException;
    
    UserDto deleteUserById(String id) throws ObjectNotFoundException;
}
