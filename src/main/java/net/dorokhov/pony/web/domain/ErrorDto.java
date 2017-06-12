package net.dorokhov.pony.web.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

public final class ErrorDto {
    
    public enum Code {
        UNEXPECTED,
        BAD_REQUEST,
        MAX_UPLOAD_SIZE_EXCEEDED,
        ACCESS_DENIED,
        VALIDATION,
        INVALID_CREDENTIALS,
        OBJECT_NOT_FOUND,
    }
    
    public static final class FieldViolation {
        
        private final String field;
        private final String code;
        private final String message;
        private final List<String> arguments;

        @JsonCreator
        public FieldViolation(String field, String code, String message, @Nullable List<String> arguments) {
            this.field = checkNotNull(field);
            this.code = checkNotNull(code);
            this.message = checkNotNull(message);
            this.arguments = arguments != null ? ImmutableList.copyOf(arguments) : emptyList();
        }

        public String getField() {
            return field;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getArguments() {
            return arguments;
        }
    }
    
    private final Code code;
    private final String message;
    private final List<String> arguments;
    private final List<FieldViolation> fieldViolations;

    @JsonCreator
    public ErrorDto(Code code, String message, @Nullable List<String> arguments, @Nullable List<FieldViolation> fieldViolations) {
        this.code = checkNotNull(code);
        this.message = checkNotNull(message);
        this.arguments = arguments != null ? ImmutableList.copyOf(arguments) : emptyList();
        this.fieldViolations = fieldViolations != null ? ImmutableList.copyOf(fieldViolations) : emptyList();
    }
    
    public ErrorDto(Code code, String message, String... arguments) {
        this(code, message, Arrays.asList(arguments), null);
    }
    
    public ErrorDto(Code code, String message, List<FieldViolation> fieldViolations) {
        this(code, message, null, checkNotNull(fieldViolations));
    }

    public Code getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public List<FieldViolation> getFieldViolations() {
        return fieldViolations;
    }
}
