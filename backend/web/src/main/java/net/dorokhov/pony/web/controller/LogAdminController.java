package net.dorokhov.pony.web.controller;

import java.time.LocalDateTime;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.dorokhov.pony.api.log.domain.LogMessage.Level;
import net.dorokhov.pony.web.domain.LogMessagePageDto;
import net.dorokhov.pony.web.service.LogFacade;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/log", produces = "application/json")
@Api(description = "Log administration operations")
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
