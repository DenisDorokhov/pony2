package net.dorokhov.pony3.web.validation;

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
@Constraint(validatedBy = InstallationSecretValidator.class)
@ReportAsSingleViolation
@NotNull
public @interface InstallationSecret {

    String message() default "must be a valid installation secret";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
