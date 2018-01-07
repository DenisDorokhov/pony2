package net.dorokhov.pony.web.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RepeatPasswordValidatorTest {
    
    private final RepeatPasswordValidator validator = new RepeatPasswordValidator();
    
    @Mock
    private ConstraintValidatorContext constraintValidatorContext;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    @Before
    public void setUp() {
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addPropertyNode(any())).thenReturn(nodeBuilderCustomizableContext);
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