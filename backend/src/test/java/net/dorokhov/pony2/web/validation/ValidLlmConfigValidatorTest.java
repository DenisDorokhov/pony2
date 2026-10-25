package net.dorokhov.pony2.web.validation;

import jakarta.validation.ConstraintValidatorContext;
import net.dorokhov.pony2.web.dto.ConfigDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
                .setDiscoveryEnabled(true)
                .setLlmUrl("https://api.openai.com/v1")
                .setLlmModel("gpt-5")
                .setLlmApiKey("some-api-key");

        assertThat(validator.isValid(config, constraintValidatorContext)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "https://api.openai.com", "https://api.openai.com/", "ftp://localhost/v1", "http:///v1"})
    public void shouldFailValidationWhenUrlIsInvalid(String url) {
        ConfigDto config = new ConfigDto()
                .setDiscoveryEnabled(true)
                .setLlmUrl(url)
                .setLlmModel("gpt-5");

        assertThat(validator.isValid(config, constraintValidatorContext)).isFalse();
        verify(constraintValidatorContext).disableDefaultConstraintViolation();
        verify(constraintValidatorContext).buildConstraintViolationWithTemplate(
                "must be an absolute HTTP(S) URL including an API path, for example https://api.openai.com/v1");
        verify(constraintViolationBuilder).addPropertyNode("llmUrl");
        verify(nodeBuilderCustomizableContext).addConstraintViolation();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    public void shouldFailValidationWhenDiscoveryIsEnabledWithoutUrl(String url) {
        ConfigDto config = new ConfigDto()
                .setDiscoveryEnabled(true)
                .setLlmUrl(url)
                .setLlmModel("gpt-5");

        assertThat(validator.isValid(config, constraintValidatorContext)).isFalse();
        verify(constraintValidatorContext).disableDefaultConstraintViolation();
        verify(constraintValidatorContext).buildConstraintViolationWithTemplate("must be configured when discovery is enabled");
        verify(constraintViolationBuilder).addPropertyNode("llmUrl");
        verify(nodeBuilderCustomizableContext).addConstraintViolation();
    }

    @Test
    public void shouldFailValidationWhenDiscoveryIsEnabledWithoutLlmConfig() {
        ConfigDto config = new ConfigDto()
                .setDiscoveryEnabled(true);

        assertThat(validator.isValid(config, constraintValidatorContext)).isFalse();
        verify(constraintValidatorContext).disableDefaultConstraintViolation();
        verify(constraintValidatorContext).buildConstraintViolationWithTemplate("must be configured when discovery is enabled");
        verify(constraintViolationBuilder).addPropertyNode("llmUrl");
        verify(nodeBuilderCustomizableContext).addConstraintViolation();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    public void shouldFailValidationWhenDiscoveryIsEnabledWithoutModel(String model) {
        ConfigDto config = new ConfigDto()
                .setDiscoveryEnabled(true)
                .setLlmUrl("https://api.openai.com/v1")
                .setLlmModel(model);

        assertThat(validator.isValid(config, constraintValidatorContext)).isFalse();
        verify(constraintValidatorContext).disableDefaultConstraintViolation();
        verify(constraintValidatorContext).buildConstraintViolationWithTemplate("must be configured when discovery is enabled");
        verify(constraintViolationBuilder).addPropertyNode("llmModel");
        verify(nodeBuilderCustomizableContext).addConstraintViolation();
    }

    @Test
    public void shouldPassValidationWithoutApiKey() {
        ConfigDto config = new ConfigDto()
                .setDiscoveryEnabled(true)
                .setLlmUrl("http://localhost:11434/v1")
                .setLlmModel("model");

        assertThat(validator.isValid(config, constraintValidatorContext)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unfinished URL", "http://localhost/v1"})
    public void shouldSkipLlmValidationWhenDiscoveryIsDisabled(String url) {
        ConfigDto config = new ConfigDto()
                .setLlmUrl(url)
                .setLlmApiKey("secret");

        assertThat(validator.isValid(config, constraintValidatorContext)).isTrue();
    }
}
