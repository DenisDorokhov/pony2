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
@Constraint(validatedBy = InstallationSecretValidator.class)
@ReportAsSingleViolation
@NotNull
public @interface InstallationSecret {

    String message() default "must be a valid installation secret";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
