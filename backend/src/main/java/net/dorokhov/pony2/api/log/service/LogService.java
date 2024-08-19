package net.dorokhov.pony2.api.log.service;

import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.log.domain.LogMessage.Level;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LogService {

    Page<LogMessage> getByTypeAndDate(Level minimalLevel, Pageable pageable);

    Optional<LogMessage> info(Logger logger, String message, Object... arguments);
    Optional<LogMessage> warn(Logger logger, String message, Object... arguments);
    Optional<LogMessage> error(Logger logger, String message, Object... arguments);
}
