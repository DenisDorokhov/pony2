package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    public AuthenticationController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }
    
    @GetMapping
    public UserDto getCurrentUser() throws NotAuthenticatedException {
        User user = currentUserService.getCurrentUser();
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
        return new UserDto(currentUserService.logout());
    }
}
