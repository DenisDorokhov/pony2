package net.dorokhov.pony2.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.dorokhov.pony2.web.dto.ConfigDto;
import org.springframework.util.StringUtils;

public class ValidLlmConfigValidator implements ConstraintValidator<ValidLlmConfig, ConfigDto> {

    @Override
    public boolean isValid(ConfigDto config, ConstraintValidatorContext context) {
        if (config == null) {
            return true;
        }
        if (!StringUtils.hasText(config.getLlmUrl())) {
            if (StringUtils.hasText(config.getLlmModel())) {
                addViolation(context, "llmUrl", "must be configured when LLM model is configured");
                return false;
            }
            if (StringUtils.hasText(config.getLlmApiKey())) {
                addViolation(context, "llmUrl", "must be configured when LLM API key is configured");
                return false;
            }
        } else if (!StringUtils.hasText(config.getLlmModel())) {
            addViolation(context, "llmModel", "must be configured when LLM URL is configured");
            return false;
        }
        return true;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
