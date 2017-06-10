package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.user.service.exception.InvalidCredentialsException;
import net.dorokhov.pony.user.service.exception.NotAuthenticatedException;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = AuthenticationController.class)
@ResponseBody
public class AuthenticationControllerAdvice {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorDto onInvalidCredentials(InvalidCredentialsException e) {
        logger.debug("Credentials are invalid.");
        return new ErrorDto(Code.INVALID_CREDENTIALS, "Credentials are invalid.");
    }
    
    @ExceptionHandler(NotAuthenticatedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorDto onNotAuthenticated(NotAuthenticatedException e) {
        logger.debug("User is not authenticated.");
        return new ErrorDto(Code.ACCESS_DENIED, "Access denied.");
    }
}
