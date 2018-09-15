package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.FrontendLogCommandDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.UNAUTHORIZED_MESSAGE;

@RestController
@RequestMapping(value = "/api/frontend", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "Frontend")
@ApiResponses({
        @ApiResponse(code = SC_UNAUTHORIZED, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
})
public class FrontendController implements ErrorHandlingController {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PostMapping("/log")
    @ApiOperation("Write log messages.")
    public void log(@RequestBody List<FrontendLogCommandDto> commands, @RequestHeader(HttpHeaders.USER_AGENT) String userAgent) {
        String message = "[{}] -> {}";
        for (FrontendLogCommandDto command : commands) {
            switch (command.getLevel()) {
                case TRACE:
                    logger.trace(message, userAgent, command.getMessage());
                    break;
                case DEBUG:
                    logger.debug(message, userAgent, command.getMessage());
                    break;
                case INFO:
                    logger.info(message, userAgent, command.getMessage());
                    break;
                case WARN:
                    logger.warn(message, userAgent, command.getMessage());
                    break;
                case ERROR:
                    logger.error(message, userAgent, command.getMessage());
                    break;
            }
        }
    }
}
