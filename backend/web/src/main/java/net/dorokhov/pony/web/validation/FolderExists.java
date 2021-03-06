package net.dorokhov.pony.web.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.constraints.NotNull;
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
