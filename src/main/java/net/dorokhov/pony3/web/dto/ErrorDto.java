package net.dorokhov.pony3.web.dto;

import java.util.ArrayList;
import java.util.List;

import static net.dorokhov.pony3.web.dto.ErrorDto.Code.AUTHENTICATION_FAILED;

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

        private String field;
        private String code;
        private String message;
        private List<String> arguments = new ArrayList<>();

        public String getField() {
            return field;
        }

        public FieldViolation setField(String field) {
            this.field = field;
            return this;
        }

        public String getCode() {
            return code;
        }

        public FieldViolation setCode(String code) {
            this.code = code;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public FieldViolation setMessage(String message) {
            this.message = message;
            return this;
        }

        public List<String> getArguments() {
            if (arguments == null) {
                arguments = new ArrayList<>();
            }
            return arguments;
        }

        public FieldViolation setArguments(List<String> arguments) {
            this.arguments = arguments;
            return this;
        }
    }

    private Code code;
    private String message;
    private List<String> arguments = new ArrayList<>();
    private List<FieldViolation> fieldViolations = new ArrayList<>();

    public Code getCode() {
        return code;
    }

    public ErrorDto setCode(Code code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ErrorDto setMessage(String message) {
        this.message = message;
        return this;
    }

    public List<String> getArguments() {
        if (arguments == null) {
            arguments = new ArrayList<>();
        }
        return arguments;
    }

    public ErrorDto setArguments(List<String> arguments) {
        this.arguments = arguments;
        return this;
    }

    public List<FieldViolation> getFieldViolations() {
        if (fieldViolations == null) {
            fieldViolations = new ArrayList<>();
        }
        return fieldViolations;
    }

    public ErrorDto setFieldViolations(List<FieldViolation> fieldViolations) {
        this.fieldViolations = fieldViolations;
        return this;
    }

    public static ErrorDto unexpected() {
        return new ErrorDto()
                .setCode(Code.UNEXPECTED)
                .setMessage("Unexpected error occurred.");
    }

    public static ErrorDto badRequest() {
        return new ErrorDto()
                .setCode(Code.BAD_REQUEST)
                .setMessage("Bad request.");
    }

    public static ErrorDto authenticationFailed() {
        return new ErrorDto()
                .setCode(AUTHENTICATION_FAILED)
                .setMessage("Authentication failed.");
    }
}
