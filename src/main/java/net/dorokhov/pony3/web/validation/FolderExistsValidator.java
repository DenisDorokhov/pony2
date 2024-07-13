package net.dorokhov.pony3.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
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
