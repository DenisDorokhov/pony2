package net.dorokhov.pony2.web.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            constraintViolationValueFields.add(valueFields.getFirst());
        }
        Set<Object> values = new HashSet<>();
        for (Field field : valueFields) {
            try {
                field.setAccessible(true);
                values.add(ReflectionUtils.getField(field, value));
            } catch (Throwable t) {
                logger.warn("Could not get repeat password field value.", t);
                return false;
            }
        }
        if (values.size() <= 1) {
            return true;
        } else {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(constraintViolationValueFields.getFirst().getName())
                    .addConstraintViolation();
            return false;
        }
    }
}
