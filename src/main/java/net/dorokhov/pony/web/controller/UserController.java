package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;
import net.dorokhov.pony.web.domain.CurrentUserUpdateCommandDto;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.service.UserContextService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController implements ErrorHandlingController {

    @ControllerAdvice(assignableTypes = UserController.class)
    @ResponseBody
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public static class Advice {

        @ExceptionHandler(InvalidPasswordException.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        public ErrorDto onInvalidPassword(InvalidPasswordException e) {
            return new ErrorDto(Code.INVALID_PASSWORD, e.getMessage());
        }

        @ExceptionHandler(DuplicateEmailException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onDuplicateEmail(DuplicateEmailException e) {
            return new ErrorDto(Code.DUPLICATE_EMAIL, e.getMessage(), e.getEmail());
        }
    }
    
    private final UserContextService userContextService;
    private final UserService userService;

    public UserController(UserContextService userContextService, UserService userService) {
        this.userContextService = userContextService;
        this.userService = userService;
    }

    @GetMapping
    public UserDto getCurrentUser() {
        return UserDto.of(userContextService.getAuthenticatedUser());
    }
    
    @PutMapping
    public UserDto updateCurrentUser(@Valid @RequestBody CurrentUserUpdateCommandDto command) 
            throws UserNotFoundException, InvalidPasswordException, DuplicateEmailException {
        User currentUser = userContextService.getAuthenticatedUser();
        return UserDto.of(userService.update(command.convert(currentUser.getId())));
    }
}
