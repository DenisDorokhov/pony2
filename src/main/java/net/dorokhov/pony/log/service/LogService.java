package net.dorokhov.pony.log.service;

import net.dorokhov.pony.log.domain.LogMessage;
import net.dorokhov.pony.log.domain.LogMessage.Level;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface LogService {

    Page<LogMessage> getByType(Level minimalLevel, Pageable pageable);
    Page<LogMessage> getByTypeAndDate(Level minimalLevel, LocalDateTime minDate, LocalDateTime maxDate, Pageable pageable);

    LogMessage debug(String message, Object... arguments);
    LogMessage info(String message, Object... arguments);
    LogMessage warn(String message, Object... arguments);
    LogMessage error(String message, Object... arguments);
}
