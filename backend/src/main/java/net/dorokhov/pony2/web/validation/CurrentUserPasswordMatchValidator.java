package net.dorokhov.pony2.web.validation;

import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.api.user.service.exception.UserNotFoundException;
import net.dorokhov.pony2.web.dto.UserDto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.dorokhov.pony2.web.service.UserFacade;

public class CurrentUserPasswordMatchValidator implements ConstraintValidator<CurrentUserPasswordMatch, String> {
    
    private final UserFacade userFacade;
    private final UserService userService;

    public CurrentUserPasswordMatchValidator(UserFacade userFacade, UserService userService) {
        this.userFacade = userFacade;
        this.userService = userService;
    }

    @Override
    public void initialize(CurrentUserPasswordMatch constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        UserDto currentUser = userFacade.getCurrentUser();
        try {
            return userService.checkUserPassword(currentUser.getId(), value);
        } catch (UserNotFoundException e) {
            throw new IllegalStateException(String.format("Current user '%s' not found.", currentUser.getId()));
        }
    }
}
