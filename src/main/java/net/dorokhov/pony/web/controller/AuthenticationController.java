package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.exception.InvalidCredentialsException;
import net.dorokhov.pony.user.service.exception.NotAuthenticatedException;
import net.dorokhov.pony.web.domain.CredentialsDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.domain.UserTokenDto;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/authentication")
public class AuthenticationController implements ResponseBodyController {
    
    private final UserService userService;

    public AuthenticationController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public UserDto getCurrentUser() throws NotAuthenticatedException {
        User user = userService.getCurrentUser();
        if (user != null) {
            return new UserDto(user);
        } else {
            throw new NotAuthenticatedException();
        }
    }

    @PostMapping
    public UserTokenDto authenticate(@Valid @RequestBody CredentialsDto credentials) throws InvalidCredentialsException {
        return new UserTokenDto(userService.authenticate(credentials.getEmail(), credentials.getPassword()));
    }
    
    @DeleteMapping
    public UserDto logout() throws NotAuthenticatedException {
        User user = userService.logout();
        if (user != null) {
            return new UserDto(user);
        } else {
            throw new NotAuthenticatedException();
        }
    }
}
