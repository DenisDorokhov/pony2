package net.dorokhov.pony3.web.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepeatPasswordValidator implements ConstraintValidator<RepeatPassword, Object> {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void initialize(RepeatPassword constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        List<Field> valueFields = new ArrayList<>();
        List<Field> constraintViolationValueFields = new ArrayList<>();
        ReflectionUtils.doWithFields(value.getClass(), field -> {
            RepeatPasswordValue[] annotations = field.getAnnotationsByType(RepeatPasswordValue.class);
            if (annotations.length > 0) {
                valueFields.add(field);
                if (annotations[0].constraintViolationField()) {
                    constraintViolationValueFields.add(field);
                }
            }
        });
        if (valueFields.size() < 2) {
            logger.warn("At least two fields of class '{}' are expected to be marked with repeat password annotation '{}'.",
                    value.getClass().getName(), RepeatPasswordValue.class.getName());
            return true;
        }
        if (constraintViolationValueFields.isEmpty()) {
            constraintViolationValueFields.add(valueFields.get(0));
        }
        Set<Object> values = valueFields.stream()
                .flatMap(field -> {
                    try {
                        field.setAccessible(true);
                        return Stream.of(ReflectionUtils.getField(field, value));
                    } catch (Exception e) {
                        logger.warn("Could not get repeat password field value.", e);
                        return Stream.of();
                    }
                })
                .collect(Collectors.toSet());
        if (values.size() <= 1) {
            return true;
        } else {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(constraintViolationValueFields.get(0).getName())
                    .addConstraintViolation();
            return false;
        }
    }
}
