package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.domain.LogDto;
import net.dorokhov.pony.web.domain.LogMessageDto;

import java.time.LocalDateTime;

public interface LogFacade {
    LogDto getLog(LogMessageDto.Level minLevel, int pageIndex);
    LogDto getLog(LogMessageDto.Level minLevel, LocalDateTime minDate, LocalDateTime maxDate, int pageIndex);
}
