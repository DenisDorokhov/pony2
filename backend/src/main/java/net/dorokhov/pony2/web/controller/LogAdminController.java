package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.api.log.domain.LogMessage.Level;
import net.dorokhov.pony2.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony2.web.dto.LogMessagePageDto;
import net.dorokhov.pony2.web.service.LogFacade;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class LogAdminController implements ErrorHandlingController {

    private final LogFacade logFacade;

    public LogAdminController(LogFacade logFacade) {
        this.logFacade = logFacade;
    }

    @GetMapping("/api/admin/log")
    public LogMessagePageDto getLog(
            @RequestParam(required = false) Level minLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime minDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime maxDate,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "30") int pageSize
    ) {
        return logFacade.getLog(minLevel, minDate, maxDate, pageIndex, pageSize);
    }
}
