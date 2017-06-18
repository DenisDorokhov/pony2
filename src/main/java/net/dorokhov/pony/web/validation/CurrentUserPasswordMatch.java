package net.dorokhov.pony.web.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Target({METHOD, FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrentUserPasswordMatchValidator.class)
@ReportAsSingleViolation
public @interface CurrentUserPasswordMatch {

    String message() default "password does not match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
