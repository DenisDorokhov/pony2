package net.dorokhov.pony.web.service;

import net.dorokhov.pony.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import net.dorokhov.pony.web.domain.CurrentUserUpdateCommandDto;
import net.dorokhov.pony.web.domain.UserCreationCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.domain.UserUpdateCommandDto;

import java.util.List;

public interface UserFacade {

    UserDto getCurrentUser();
    UserDto updateCurrentUser(CurrentUserUpdateCommandDto command) throws 
            UserNotFoundException, InvalidPasswordException, DuplicateEmailException;
    
    List<UserDto> getAllUsers();
    
    UserDto getUserById(Long id) throws ObjectNotFoundException;
    
    UserDto createUser(UserCreationCommandDto command) throws DuplicateEmailException;
    UserDto updateUser(UserUpdateCommandDto command) throws ObjectNotFoundException, DuplicateEmailException;
}
