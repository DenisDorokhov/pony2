package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.web.domain.CurrentUserUpdateCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.service.UserFacade;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController implements ErrorHandlingController {
    
    private final UserFacade userFacade;

    public UserController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @GetMapping
    public UserDto getCurrentUser() {
        return userFacade.getCurrentUser();
    }
    
    @PutMapping
    public UserDto updateCurrentUser(@Valid @RequestBody CurrentUserUpdateCommandDto command) throws InvalidPasswordException, DuplicateEmailException {
        return userFacade.updateCurrentUser(command);
    }
}
