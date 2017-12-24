package net.dorokhov.pony.web.service;

import net.dorokhov.pony.api.log.domain.LogMessage.Level;
import net.dorokhov.pony.web.domain.LogMessagePageDto;

import java.time.LocalDateTime;

public interface LogFacade {
    LogMessagePageDto getLog(Level minLevel, int pageIndex);
    LogMessagePageDto getLog(Level minLevel, LocalDateTime minDate, LocalDateTime maxDate, int pageIndex);
}
