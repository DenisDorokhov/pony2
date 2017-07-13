package net.dorokhov.pony.web.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.io.File;

public final class FolderExistsValidator implements ConstraintValidator<FolderExists, String> {

    @Override
    public void initialize(FolderExists annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return new File(value).isDirectory();
    }
}
