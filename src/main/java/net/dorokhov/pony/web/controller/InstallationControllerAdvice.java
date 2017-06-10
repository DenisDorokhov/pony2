package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = InstallationController.class)
@ResponseBody
public class InstallationControllerAdvice {
    
    @ExceptionHandler(AlreadyInstalledException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto onAlreadyInstalled() {
        return new ErrorDto(Code.BAD_REQUEST, "Bad request.");
    }
}
