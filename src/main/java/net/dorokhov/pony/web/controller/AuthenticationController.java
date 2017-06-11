package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserAuthenticationService;
import net.dorokhov.pony.user.service.UserContextService;
import net.dorokhov.pony.user.service.exception.InvalidCredentialsException;
import net.dorokhov.pony.user.service.exception.NotAuthenticatedException;
import net.dorokhov.pony.web.domain.CredentialsDto;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.domain.UserTokenDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/authentication")
public class AuthenticationController implements ResponseBodyController {

    @ControllerAdvice(assignableTypes = AuthenticationController.class)
    @ResponseBody
    public static class Advice {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        @ExceptionHandler(InvalidCredentialsException.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        public ErrorDto onInvalidCredentials(InvalidCredentialsException e) {
            logger.debug("Credentials are invalid.");
            return new ErrorDto(ErrorDto.Code.INVALID_CREDENTIALS, "Credentials are invalid.");
        }

        @ExceptionHandler(NotAuthenticatedException.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        public ErrorDto onNotAuthenticated(NotAuthenticatedException e) {
            logger.debug("User is not authenticated.");
            return new ErrorDto(ErrorDto.Code.ACCESS_DENIED, "Access denied.");
        }
    }
    
    private final UserAuthenticationService userAuthenticationService;
    private final UserContextService userContextService;

    public AuthenticationController(UserAuthenticationService useruserAuthenticationService, UserContextService userContextService) {
        this.userAuthenticationService = useruserAuthenticationService;
        this.userContextService = userContextService;
    }
    
    @GetMapping
    public UserDto getCurrentUser() throws NotAuthenticatedException {
        User user = userContextService.getUser();
        if (user != null) {
            return new UserDto(user);
        } else {
            throw new NotAuthenticatedException();
        }
    }

    @PostMapping
    public UserTokenDto authenticate(@Valid @RequestBody CredentialsDto credentials) throws InvalidCredentialsException {
        return new UserTokenDto(userAuthenticationService.authenticate(credentials.getEmail(), credentials.getPassword()));
    }
    
    @DeleteMapping
    public UserDto logout() throws NotAuthenticatedException {
        return new UserDto(userContextService.clearUser());
    }
}
