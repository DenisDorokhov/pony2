package net.dorokhov.pony2.api.log.service;

import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.log.domain.LogMessage.Level;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LogService {

    Page<LogMessage> getByTypeAndDate(Level minimalLevel, LocalDateTime minDate, LocalDateTime maxDate, Pageable pageable);

    Optional<LogMessage> debug(Logger logger, String message, Object... arguments);
    Optional<LogMessage> info(Logger logger, String message, Object... arguments);
    Optional<LogMessage> warn(Logger logger, String message, Object... arguments);
    Optional<LogMessage> error(Logger logger, String message, Object... arguments);
}
