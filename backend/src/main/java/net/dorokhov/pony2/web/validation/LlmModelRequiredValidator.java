package net.dorokhov.pony2.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.dorokhov.pony2.web.dto.ConfigDto;
import org.springframework.util.StringUtils;

public class LlmModelRequiredValidator implements ConstraintValidator<LlmModelRequired, ConfigDto> {

    @Override
    public boolean isValid(ConfigDto config, ConstraintValidatorContext context) {
        if (config == null || !StringUtils.hasText(config.getLlmUrl()) || StringUtils.hasText(config.getLlmModel())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("llmModel")
                .addConstraintViolation();
        return false;
    }
}
