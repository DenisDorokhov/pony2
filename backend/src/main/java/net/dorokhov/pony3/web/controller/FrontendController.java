package net.dorokhov.pony3.web.controller;

import net.dorokhov.pony3.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony3.web.dto.FrontendLogCommandDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/frontend", produces = MediaType.APPLICATION_JSON_VALUE)
public class FrontendController implements ErrorHandlingController {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PostMapping("/log")
    public void log(@RequestBody List<FrontendLogCommandDto> commands, @RequestHeader(HttpHeaders.USER_AGENT) String userAgent) {
        String message = "[{}] {} ('{}')";
        for (FrontendLogCommandDto command : commands) {
            switch (command.getLevel()) {
                case TRACE:
                    logger.trace(message, command.getDate(), command.getMessage(), userAgent);
                    break;
                case DEBUG:
                    logger.debug(message, command.getDate(), command.getMessage(), userAgent);
                    break;
                case INFO:
                    logger.info(message, command.getDate(), command.getMessage(), userAgent);
                    break;
                case WARN:
                    logger.warn(message, command.getDate(), command.getMessage(), userAgent);
                    break;
                case ERROR:
                    logger.error(message, command.getDate(), command.getMessage(), userAgent);
                    break;
            }
        }
    }
}
