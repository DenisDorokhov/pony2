package net.dorokhov.pony3.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony3.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony3.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony3.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony3.web.dto.CurrentUserUpdateCommandDto;
import net.dorokhov.pony3.web.dto.UserDto;
import net.dorokhov.pony3.web.service.UserFacade;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/user", produces = APPLICATION_JSON_VALUE)
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
