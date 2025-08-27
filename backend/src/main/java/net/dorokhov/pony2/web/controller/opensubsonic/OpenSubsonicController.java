package net.dorokhov.pony2.web.controller.opensubsonic;

import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static net.dorokhov.pony2.web.service.OpenSubsonicResponseService.ERROR_GENERIC;
import static net.dorokhov.pony2.web.service.OpenSubsonicResponseService.ERROR_NOT_FOUND;

public interface OpenSubsonicController {

    @ControllerAdvice(assignableTypes = OpenSubsonicController.class)
    @ResponseBody
    class Advice {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final OpenSubsonicResponseService responseFactory;

        public Advice(OpenSubsonicResponseService responseFactory) {
            this.responseFactory = responseFactory;
        }

        @ExceptionHandler(ObjectNotFoundException.class)
        @ResponseStatus(HttpStatus.OK)
        public Object onObjectNotFound(ObjectNotFoundException e) {
            logger.info("Object not found.", e);
            return responseFactory.createError(ERROR_NOT_FOUND, "Object not found.");
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.OK)
        public Object onUnexpectedError(Exception e) {
            logger.error("Unexpected error occurred.", e);
            return responseFactory.createError(ERROR_GENERIC, "Unexpected error occurred.");
        }
    }
}
