package net.dorokhov.pony2.web.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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