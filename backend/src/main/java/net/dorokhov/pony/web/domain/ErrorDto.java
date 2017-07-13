package net.dorokhov.pony.web.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static net.dorokhov.pony.web.domain.ErrorDto.Code.AUTHENTICATION_FAILED;

public final class ErrorDto {

    public enum Code {
        UNEXPECTED,
        BAD_REQUEST,
        AUTHENTICATION_FAILED,
        ACCESS_DENIED,
        VALIDATION,
        NOT_FOUND,
        MAX_UPLOAD_SIZE_EXCEEDED,
        CONCURRENT_SCAN,
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
            this.arguments = arguments != null ? unmodifiableList(arguments) : emptyList();
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
        this.arguments = arguments != null ? unmodifiableList(arguments) : emptyList();
        this.fieldViolations = fieldViolations != null ? unmodifiableList(fieldViolations) : emptyList();
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

    public static ErrorDto unexpected() {
        return new ErrorDto(ErrorDto.Code.UNEXPECTED, "Unexpected error occurred.");
    }

    public static ErrorDto badRequest() {
        return new ErrorDto(ErrorDto.Code.BAD_REQUEST, "Bad request.");
    }

    public static ErrorDto authenticationFailed() {
        return new ErrorDto(AUTHENTICATION_FAILED, "Authentication failed.");
    }
}
