package net.dorokhov.pony2.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RepeatPasswordValidator.class)
@ReportAsSingleViolation
public @interface RepeatPassword {

    String message() default "must match entered password";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
