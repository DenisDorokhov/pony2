package net.dorokhov.pony.web.controller;

import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.dorokhov.pony.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.web.domain.CurrentUserUpdateCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.service.UserFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/user", produces = "application/json")
@Api(description = "Current user operations")
public class UserController implements ErrorHandlingController {
    
    private final UserFacade userFacade;

    public UserController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @GetMapping
    @ApiOperation("Get current user.")
    public UserDto getCurrentUser() {
        return userFacade.getCurrentUser();
    }
    
    @PutMapping
    @ApiOperation("Update current user.")
    public UserDto updateCurrentUser(@Valid @RequestBody CurrentUserUpdateCommandDto command) throws InvalidPasswordException, DuplicateEmailException {
        return userFacade.updateCurrentUser(command);
    }
}
