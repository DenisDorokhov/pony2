package net.dorokhov.pony2.core.log.service;

import com.google.common.base.Throwables;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.log.domain.LogMessage.Level;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.log.repository.LogMessageRepository;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class LogServiceImpl implements LogService {

    private final LogMessageRepository logMessageRepository;

    public LogServiceImpl(LogMessageRepository logMessageRepository) {
        this.logMessageRepository = logMessageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogMessage> getByTypeAndDate(Level minimalLevel, LocalDateTime minDate, LocalDateTime maxDate, Pageable pageable) {
        return logMessageRepository.findByLevelInAndDateBetween(minimalLevel.getIncludedLevels(), minDate, maxDate, pageable);
    }

    @Override
    @Transactional
    public LogMessage debug(Logger logger, String message, Object... arguments) {
        logger.debug(message, arguments);
        return doLog(Level.DEBUG, message, arguments);
    }

    @Override
    @Transactional
    public LogMessage info(Logger logger, String message, Object... arguments) {
        logger.info(message, arguments);
        return doLog(Level.INFO, message, arguments);
    }

    @Override
    @Transactional
    public LogMessage warn(Logger logger, String message, Object... arguments) {
        logger.warn(message, arguments);
        return doLog(Level.WARN, message, arguments);
    }

    @Override
    @Transactional
    public LogMessage error(Logger logger, String message, Object... arguments) {
        logger.error(message, arguments);
        return doLog(Level.ERROR, message, arguments);
    }

    private LogMessage doLog(Level level, String pattern, Object... arguments) {
        List<String> stringArguments = Arrays.stream(arguments)
                .map(o -> {
                    if (o instanceof Throwable) {
                        return Throwables.getStackTraceAsString((Throwable) o);
                    } else {
                        return o != null ? o.toString() : null;
                    }
                })
                .toList();
        return logMessageRepository.save(new LogMessage()
                .setLevel(level)
                .setPattern(pattern)
                .setArguments(stringArguments)
                .setText(MessageFormatter.arrayFormat(pattern, arguments).getMessage())
        );
    }
}
