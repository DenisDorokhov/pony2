package net.dorokhov.pony.web.controller.common;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.web.controller.exception.BadRequestException;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public interface ErrorHandlingController {

    @ControllerAdvice(assignableTypes = ErrorHandlingController.class)
    @ResponseBody
    class Advice {

        protected final Logger logger = LoggerFactory.getLogger(getClass());

        @ExceptionHandler(MethodArgumentNotValidException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onValidationError(MethodArgumentNotValidException e) {
            List<ErrorDto.FieldViolation> fieldViolations = new ArrayList<>();
            for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
                List<String> errorArguments = new ArrayList<>();
                for (Object argument : fieldError.getArguments()) {
                    if (argument instanceof Byte
                            || argument instanceof Short
                            || argument instanceof Integer
                            || argument instanceof Long
                            || argument instanceof Float
                            || argument instanceof Double
                            || argument instanceof Character
                            || argument instanceof Boolean
                            || argument instanceof String
                            ) {
                        errorArguments.add(argument.toString());
                    }
                }
                fieldViolations.add(new ErrorDto.FieldViolation(
                        fieldError.getField(),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, fieldError.getCode()), 
                        fieldError.getDefaultMessage(), 
                        errorArguments
                ));
            }
            return new ErrorDto(Code.VALIDATION, "Invalid request, check field violations.", fieldViolations);
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
            return new ErrorDto(Code.MAX_UPLOAD_SIZE_EXCEEDED, e.getMessage(), String.valueOf(e.getMaxUploadSize()));
        }

        @ExceptionHandler({
                HttpMediaTypeException.class,
                HttpMessageNotReadableException.class,
                MissingServletRequestParameterException.class,
                BadRequestException.class
        })
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onBadRequest() {
            return ErrorDto.badRequest();
        }

        @ExceptionHandler(ObjectNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ErrorDto onObjectNotFound(ObjectNotFoundException e) {
            ImmutableList.Builder<String> argumentsBuilder = ImmutableList.builder();
            argumentsBuilder.add(e.getObjectType().getSimpleName());
            if (e.getObjectId() != null) {
                argumentsBuilder.add(e.getObjectId().toString());
            }
            return new ErrorDto(Code.NOT_FOUND, e.getMessage(), argumentsBuilder.build(), emptyList());
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public Object onUnexpectedError(Exception e) {
            if (e.getCause() instanceof IOException) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Avoid logging of broken pipe exceptions.
            }
            logger.error("Unexpected error occurred.", e);
            return ErrorDto.unexpected();
        }
    }
}
