package net.dorokhov.pony2.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.dorokhov.pony2.web.dto.ConfigDto;
import org.springframework.util.StringUtils;

import java.net.URI;

public class ValidLlmConfigValidator implements ConstraintValidator<ValidLlmConfig, ConfigDto> {

    @Override
    public boolean isValid(ConfigDto config, ConstraintValidatorContext context) {
        if (config == null || !config.isDiscoveryEnabled()) {
            return true;
        }
        if (!StringUtils.hasText(config.getLlmUrl())) {
            addViolation(context, "llmUrl", "must be configured when discovery is enabled");
            return false;
        } else if (!StringUtils.hasText(config.getLlmModel())) {
            addViolation(context, "llmModel", "must be configured when discovery is enabled");
            return false;
        } else if (!isValidLlmUrl(config.getLlmUrl())) {
            addViolation(context, "llmUrl",
                    "must be an absolute HTTP(S) URL including an API path, for example https://api.openai.com/v1");
            return false;
        }
        return true;
    }

    private boolean isValidLlmUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            String path = uri.getPath();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) &&
                    StringUtils.hasText(uri.getHost()) &&
                    StringUtils.hasText(path) &&
                    !"/".equals(path);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
