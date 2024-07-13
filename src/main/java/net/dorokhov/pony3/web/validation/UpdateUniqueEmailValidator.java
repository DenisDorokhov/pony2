package net.dorokhov.pony3.web.validation;

import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.api.user.service.UserService;
import net.dorokhov.pony3.web.dto.UserUpdateCommandDto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UpdateUniqueEmailValidator implements ConstraintValidator<UpdateUniqueEmail, UserUpdateCommandDto> {

    private final UserService userService;

    public UpdateUniqueEmailValidator(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void initialize(UpdateUniqueEmail constraintAnnotation) {
    }

    @Override
    public boolean isValid(UserUpdateCommandDto command, ConstraintValidatorContext context) {
        User currentUser = userService.getById(command.getId()).orElse(null);
        User sameEmailUser = userService.getByEmail(command.getEmail()).orElse(null);
        if (currentUser == null 
                || sameEmailUser == null 
                || sameEmailUser.getId().equals(currentUser.getId())) {
            return true;
        } else {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("email")
                    .addConstraintViolation();
            return false;
        }
    }
}
