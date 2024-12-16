package net.dorokhov.pony2.web.controller.common;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.web.controller.exception.BadRequestException;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.dto.ErrorDto.Code;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import net.dorokhov.pony2.web.service.exception.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;
import static net.dorokhov.pony2.web.dto.ErrorDto.Code.ACCESS_DENIED;

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
                fieldViolations.add(new ErrorDto.FieldViolation()
                        .setField(fieldError.getField())
                        .setCode(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, firstNonNull(fieldError.getCode(), "UNKNOWN")))
                        .setMessage(fieldError.getDefaultMessage())
                        .setArguments(errorArguments));
            }
            return new ErrorDto()
                    .setCode(Code.VALIDATION)
                    .setMessage("Invalid request, check field violations.")
                    .setFieldViolations(fieldViolations);
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
            return new ErrorDto()
                    .setCode(Code.MAX_UPLOAD_SIZE_EXCEEDED)
                    .setMessage(e.getMessage())
                    .setArguments(List.of(String.valueOf(e.getMaxUploadSize())));
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
                argumentsBuilder.add(e.getObjectId());
            }
            return new ErrorDto()
                    .setCode(Code.NOT_FOUND)
                    .setMessage(e.getMessage())
                    .setArguments(argumentsBuilder.build());
        }

        @ExceptionHandler(AccessDeniedException.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public Object onAccessDeniedError(AccessDeniedException e) {
            return new ErrorDto()
                    .setCode(ACCESS_DENIED)
                    .setMessage("Access denied.");
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public Object onUnexpectedError(Exception e) {
            logger.error("Unexpected error occurred.", e);
            return ErrorDto.unexpected();
        }
    }
}
