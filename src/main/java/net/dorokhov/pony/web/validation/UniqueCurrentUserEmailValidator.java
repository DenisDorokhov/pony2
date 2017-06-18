package net.dorokhov.pony.web.validation;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.service.UserFacade;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UniqueCurrentUserEmailValidator implements ConstraintValidator<UniqueCurrentUserEmail, String> {

    private final UserFacade userFacade;
    private final UserService userService;

    public UniqueCurrentUserEmailValidator(UserFacade userFacade, UserService userService) {
        this.userFacade = userFacade;
        this.userService = userService;
    }

    @Override
    public void initialize(UniqueCurrentUserEmail constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        UserDto currentUser = userFacade.getCurrentUser();
        User sameEmailUser = userService.getByEmail(value);
        return sameEmailUser == null || sameEmailUser.getId().equals(currentUser.getId());
    }
}
