package net.dorokhov.pony2.web.validation;

import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.web.dto.UserDto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.dorokhov.pony2.web.service.UserFacade;

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
        User sameEmailUser = userService.getByEmail(value).orElse(null);
        return sameEmailUser == null || sameEmailUser.getId().equals(currentUser.getId());
    }
}
