package net.dorokhov.pony.web.validation;

import net.dorokhov.pony.api.user.service.UserService;
import net.dorokhov.pony.api.user.service.exception.UserNotFoundException;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.service.UserFacade;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
            throw new IllegalStateException(String.format("Current user '%d' not found.", currentUser.getId()));
        }
    }
}
