package net.dorokhov.pony2.web.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RepeatPasswordValidatorTest {
    
    private final RepeatPasswordValidator validator = new RepeatPasswordValidator();
    
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
    public void shouldPassValidationWhenLessThanTwoAnnotationsPresent() {
        Object target = new TargetWithOneAnnotationPresent("somePassword", "notMatchingPassword");
        assertThat(validator.isValid(target, constraintValidatorContext)).isTrue();
    }

    @Test
    public void shouldPassValidationWhenFieldsMatch() {
        Object target = new ValidTarget("somePassword", "somePassword");
        assertThat(validator.isValid(target, constraintValidatorContext)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldFailValidationWhenReadingFieldFails(boolean error) throws NoSuchFieldException {
        Object target = new ValidTarget("somePassword", "somePassword");
        Field field = ValidTarget.class.getDeclaredField("repeatPassword");
        field.setAccessible(true);
        Throwable failure = error ? new AssertionError("Could not read field") : new IllegalStateException("Could not read field");
        try (MockedStatic<ReflectionUtils> reflectionUtils = mockStatic(ReflectionUtils.class, CALLS_REAL_METHODS)) {
            reflectionUtils.when(() -> ReflectionUtils.getField(field, target)).thenThrow(failure);

            assertThat(validator.isValid(target, constraintValidatorContext)).isFalse();
        }
    }

    @Test
    public void shouldFailValidationWhenFieldsDoNotMatch() {
        Object target = new ValidTarget("somePassword", "notMatchingPassword");
        assertThat(validator.isValid(target, constraintValidatorContext)).isFalse();
        verify(constraintViolationBuilder).addPropertyNode("repeatPassword");
    }

    @Test
    public void shouldTakeAnyConstraintViolationFieldIfItIsNotDefined() {
        Object target = new ValidTarget("somePassword", "notMatchingPassword");
        assertThat(validator.isValid(target, constraintValidatorContext)).isFalse();
        verify(constraintViolationBuilder).addPropertyNode(any());
    }
    
    private static class TargetWithOneAnnotationPresent {

        @RepeatPasswordValue
        private final String password;
        private final String repeatPassword;

        private TargetWithOneAnnotationPresent(String password, String repeatPassword) {
            this.password = password;
            this.repeatPassword = repeatPassword;
        }

        public String getPassword() {
            return password;
        }

        public String getRepeatPassword() {
            return repeatPassword;
        }
    }
    
    private static class ValidTarget {
        
        @RepeatPasswordValue
        private final String password;
        @RepeatPasswordValue(constraintViolationField = true)
        private final String repeatPassword;

        private ValidTarget(String password, String repeatPassword) {
            this.password = password;
            this.repeatPassword = repeatPassword;
        }

        public String getPassword() {
            return password;
        }

        public String getRepeatPassword() {
            return repeatPassword;
        }
    }

    private static class TargetWithNoConstraintViolationField {

        @RepeatPasswordValue
        private final String password;
        @RepeatPasswordValue
        private final String repeatPassword;

        private TargetWithNoConstraintViolationField(String password, String repeatPassword) {
            this.password = password;
            this.repeatPassword = repeatPassword;
        }

        public String getPassword() {
            return password;
        }

        public String getRepeatPassword() {
            return repeatPassword;
        }
    }
}
