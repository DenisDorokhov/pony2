package net.dorokhov.pony2.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Target({METHOD, FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FolderExistsValidator.class)
@ReportAsSingleViolation
@NotNull
public @interface FolderExists {

    String message() default "must be an existing folder";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
