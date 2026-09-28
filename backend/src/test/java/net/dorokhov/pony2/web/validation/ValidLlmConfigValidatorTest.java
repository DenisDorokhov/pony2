package net.dorokhov.pony2.web.validation;

import jakarta.validation.ConstraintValidatorContext;
import net.dorokhov.pony2.web.dto.ConfigDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ValidLlmConfigValidatorTest {

    private final ValidLlmConfigValidator validator = new ValidLlmConfigValidator();

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    @BeforeEach
    public void setUp() {
        lenient().when(constraintValidatorContext.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilder);
        lenient().when(constraintViolationBuilder.addPropertyNode(any())).thenReturn(nodeBuilderCustomizableContext);
    }

    @Test
    public void shouldPassValidationWhenConfigIsNull() {
        assertThat(validator.isValid(null, constraintValidatorContext)).isTrue();
    }

    @Test
    public void shouldPassValidationWhenLlmConfigIsEmpty() {
        ConfigDto config = new ConfigDto()
                .setLlmUrl(" ")
                .setLlmModel(" ")
                .setLlmApiKey(" ");

        assertThat(validator.isValid(config, constraintValidatorContext)).isTrue();
    }

    @Test
    public void shouldPassValidationWhenUrlAndModelAreConfigured() {
        ConfigDto config = new ConfigDto()
                .setLlmUrl("https://api.openai.com")
                .setLlmModel("gpt-5")
                .setLlmApiKey("some-api-key");

        assertThat(validator.isValid(config, constraintValidatorContext)).isTrue();
    }

    @Test
    public void shouldFailValidationWhenModelIsConfiguredWithoutUrl() {
        ConfigDto config = new ConfigDto()
                .setLlmModel("gpt-5");

        assertThat(validator.isValid(config, constraintValidatorContext)).isFalse();
        verify(constraintValidatorContext).disableDefaultConstraintViolation();
        verify(constraintValidatorContext).buildConstraintViolationWithTemplate("must be configured when LLM model is configured");
        verify(constraintViolationBuilder).addPropertyNode("llmUrl");
        verify(nodeBuilderCustomizableContext).addConstraintViolation();
    }

    @Test
    public void shouldFailValidationWhenApiKeyIsConfiguredWithoutUrl() {
        ConfigDto config = new ConfigDto()
                .setLlmApiKey("some-api-key");

        assertThat(validator.isValid(config, constraintValidatorContext)).isFalse();
        verify(constraintValidatorContext).disableDefaultConstraintViolation();
        verify(constraintValidatorContext).buildConstraintViolationWithTemplate("must be configured when LLM API key is configured");
        verify(constraintViolationBuilder).addPropertyNode("llmUrl");
        verify(nodeBuilderCustomizableContext).addConstraintViolation();
    }

    @Test
    public void shouldFailValidationWhenUrlIsConfiguredWithoutModel() {
        ConfigDto config = new ConfigDto()
                .setLlmUrl("https://api.openai.com");

        assertThat(validator.isValid(config, constraintValidatorContext)).isFalse();
        verify(constraintValidatorContext).disableDefaultConstraintViolation();
        verify(constraintValidatorContext).buildConstraintViolationWithTemplate("must be configured when LLM URL is configured");
        verify(constraintViolationBuilder).addPropertyNode("llmModel");
        verify(nodeBuilderCustomizableContext).addConstraintViolation();
    }
}
