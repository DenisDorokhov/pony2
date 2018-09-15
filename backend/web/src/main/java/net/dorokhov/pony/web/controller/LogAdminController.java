package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.api.log.domain.LogMessage.Level;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.LogMessagePageDto;
import net.dorokhov.pony.web.service.LogFacade;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.FORBIDDEN_MESSAGE;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.UNAUTHORIZED_MESSAGE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/admin/log", produces = APPLICATION_JSON_VALUE)
@Api(tags = "Log Administration")
@ApiResponses({
        @ApiResponse(code = SC_UNAUTHORIZED, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
        @ApiResponse(code = SC_FORBIDDEN, message = FORBIDDEN_MESSAGE, response = ErrorDto.class),
})
public class LogAdminController implements ErrorHandlingController {

    private final LogFacade logFacade;

    public LogAdminController(LogFacade logFacade) {
        this.logFacade = logFacade;
    }

    @GetMapping
    @ApiOperation("Get page of log messages.")
    public LogMessagePageDto getLog(@RequestParam(required = false) Level minLevel,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime minDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime maxDate,
                                    @RequestParam(defaultValue = "0") int pageIndex) {
        if (minLevel == null) {
            minLevel = Level.INFO;
        }
        if (minDate != null && maxDate != null) {
            return logFacade.getLog(minLevel, minDate, maxDate, pageIndex);
        } else {
            return logFacade.getLog(minLevel, pageIndex);
        }
    }
}
