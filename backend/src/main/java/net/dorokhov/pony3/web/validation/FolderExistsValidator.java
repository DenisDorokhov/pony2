package net.dorokhov.pony3.web.validation;

import com.google.common.base.Strings;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class FolderExistsValidator implements ConstraintValidator<FolderExists, String> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void initialize(FolderExists annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (Strings.emptyToNull(value) == null) {
            return false;
        }
        try {
            return Files.isDirectory(Path.of(value));
        } catch (InvalidPathException e) {
            return false;
        } catch (Exception e) {
            logger.debug("Could not check if folder exists.", e);
            return false;
        }
    }
}
