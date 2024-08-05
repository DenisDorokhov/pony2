package net.dorokhov.pony3.api.log.service;

import net.dorokhov.pony3.api.log.domain.LogMessage;
import net.dorokhov.pony3.api.log.domain.LogMessage.Level;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface LogService {

    Page<LogMessage> getByTypeAndDate(Level minimalLevel, LocalDateTime minDate, LocalDateTime maxDate, Pageable pageable);

    LogMessage debug(Logger logger, String message, Object... arguments);
    LogMessage info(Logger logger, String message, Object... arguments);
    LogMessage warn(Logger logger, String message, Object... arguments);
    LogMessage error(Logger logger, String message, Object... arguments);
}
