package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = InstallationController.class)
@ResponseBody
public class InstallationControllerAdvice {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @ExceptionHandler(AlreadyInstalledException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto onAlreadyInstalled() {
        logger.warn("Application is already installed.");
        return new ErrorDto(Code.BAD_REQUEST, "Bad request.");
    }
}
