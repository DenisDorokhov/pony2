package net.dorokhov.pony2.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony2.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony2.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony2.web.dto.CurrentUserUpdateCommandDto;
import net.dorokhov.pony2.web.dto.OpenSubsonicApiKeyDto;
import net.dorokhov.pony2.web.dto.UserDto;
import net.dorokhov.pony2.web.service.UserFacade;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class UserController implements ErrorHandlingController {
    
    private final UserFacade userFacade;

    public UserController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @GetMapping("/api/user")
    public UserDto getCurrentUser() {
        return userFacade.getCurrentUser();
    }
    
    @PutMapping("/api/user")
    public UserDto updateCurrentUser(@Valid @RequestBody CurrentUserUpdateCommandDto command) throws InvalidPasswordException, DuplicateEmailException {
        return userFacade.updateCurrentUser(command);
    }

    @GetMapping("/api/user/openSubsonicApiKey")
    public OpenSubsonicApiKeyDto generateCurrentUserOpenSubsonicApiKey() {
        return userFacade.generateCurrentUserOpenSubsonicApiKey();
    }
}
