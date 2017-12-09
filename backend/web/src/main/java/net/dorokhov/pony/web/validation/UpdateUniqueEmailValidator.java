package net.dorokhov.pony.web.validation;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.web.domain.UserUpdateCommandDto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
        User currentUser = userService.getById(command.getId());
        User sameEmailUser = userService.getByEmail(command.getEmail());
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
