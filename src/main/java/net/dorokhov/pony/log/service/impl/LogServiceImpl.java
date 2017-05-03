package net.dorokhov.pony.log.service.impl;

import com.google.common.base.Throwables;
import net.dorokhov.pony.log.domain.LogMessage;
import net.dorokhov.pony.log.domain.LogMessage.Level;
import net.dorokhov.pony.log.repository.LogMessageRepository;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogServiceImpl implements LogService {

    private final LogMessageRepository logMessageRepository;

    private LogService self;

    public LogServiceImpl(LogMessageRepository logMessageRepository) {
        this.logMessageRepository = logMessageRepository;
    }

    @Autowired
    void setSelf(LogService self) {
        this.self = self;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogMessage> getByType(Level minimalLevel, Pageable pageable) {
        return logMessageRepository.findByLevelGreaterThanEqual(minimalLevel, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogMessage> getByTypeAndDate(Level minimalLevel, LocalDateTime minDate, LocalDateTime maxDate, Pageable pageable) {
        return logMessageRepository.findByLevelGreaterThanEqualAndDateBetween(minimalLevel, minDate, maxDate, pageable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage debug(String message, Object... arguments) {
        LoggerFactory.getLogger(fetchCallerClassName()).debug(message, arguments);
        return doLog(Level.DEBUG, message, arguments);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage info(String message, Object... arguments) {
        LoggerFactory.getLogger(fetchCallerClassName()).info(message, arguments);
        return doLog(Level.INFO, message, arguments);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage warn(String message, Object... arguments) {
        LoggerFactory.getLogger(fetchCallerClassName()).warn(message, arguments);
        return doLog(Level.WARN, message, arguments);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage error(String message, Object... arguments) {
        LoggerFactory.getLogger(fetchCallerClassName()).error(message, arguments);
        return doLog(Level.ERROR, message, arguments);
    }

    private LogMessage doLog(Level level, String pattern, Object... arguments) {
        List<String> stringArguments = Arrays.stream(arguments)
                .map(o -> {
                    if (o instanceof Throwable) {
                        return Throwables.getStackTraceAsString((Throwable) o);
                    } else {
                        return o.toString();
                    }
                })
                .collect(Collectors.toList());
        return logMessageRepository.save(LogMessage.builder()
                .type(level)
                .pattern(pattern)
                .arguments(stringArguments)
                .text(MessageFormatter.arrayFormat(pattern, arguments).getMessage())
                .build());
    }

    private String fetchCallerClassName() {
        boolean selfFound = false;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            boolean isSelf = element.getClassName().equals(self.getClass().getName());
            if (selfFound) {
                if (!isSelf) {
                    return element.getClassName();
                }
            } else {
                selfFound = isSelf;
            }
        }
        return getClass().getName();
    }
}
