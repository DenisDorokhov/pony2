package net.dorokhov.pony.logging;

import com.google.common.base.Throwables;
import net.dorokhov.pony.entity.LogMessage;
import net.dorokhov.pony.repository.LogMessageRepository;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class LogServiceImpl implements LogService {

    private final LogMessageRepository logMessageRepository;

    public LogServiceImpl(LogMessageRepository logMessageRepository) {
        this.logMessageRepository = logMessageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogMessage> getByType(LogMessage.Type minimalType, Pageable pageable) {
        return logMessageRepository.findByTypeGreaterThanEqual(minimalType, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogMessage> getByTypeAndDate(LogMessage.Type minimalType, LocalDateTime minDate, LocalDateTime maxDate, Pageable pageable) {
        return logMessageRepository.findByTypeGreaterThanEqualAndDateBetween(minimalType, minDate, maxDate, pageable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage debug(Logger logger, String code, String text) {
        return debug(logger, code, text, (String)null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage debug(Logger logger, String code, List<String> arguments, String text) {
        return debug(logger, code, arguments, text, (String)null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage debug(Logger logger, String code, String text, Throwable throwable) {
        return debug(logger, code, null, text, throwable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage debug(Logger logger, String code, List<String> arguments, String text, Throwable throwable) {
        checkNotNull(throwable);
        return debug(logger, code, arguments, text, Throwables.getStackTraceAsString(throwable));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage debug(Logger logger, String code, String text, String details) {
        return debug(logger, code, null, text, details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage debug(Logger logger, String code, List<String> arguments, String text, String details) {
        checkNotNull(code);
        return doLogMessage(logger, LogMessage.Type.DEBUG, code, text, arguments, details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage info(Logger logger, String code, String text) {
        return info(logger, code, text, (String)null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage info(Logger logger, String code, List<String> arguments, String text) {
        return info(logger, code, arguments, text, (String)null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage info(Logger logger, String code, String text, Throwable throwable) {
        return info(logger, code, null, text, throwable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage info(Logger logger, String code, List<String> arguments, String text, Throwable throwable) {
        checkNotNull(throwable);
        return info(logger, code, arguments, text, Throwables.getStackTraceAsString(throwable));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage info(Logger logger, String code, String text, String details) {
        return info(logger, code, null, text, details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage info(Logger logger, String code, List<String> arguments, String text, String details) {
        checkNotNull(code);
        return doLogMessage(logger, LogMessage.Type.INFO, code, text, arguments, details);
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage warn(Logger logger, String code, String text) {
        return warn(logger, code, text, (String)null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage warn(Logger logger, String code, List<String> arguments, String text) {
        return warn(logger, code, arguments, text, (String)null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage warn(Logger logger, String code, String text, Throwable throwable) {
        return warn(logger, code, null, text, throwable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage warn(Logger logger, String code, List<String> arguments, String text, Throwable throwable) {
        checkNotNull(throwable);
        return warn(logger, code, arguments, text, Throwables.getStackTraceAsString(throwable));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage warn(Logger logger, String code, String text, String details) {
        return warn(logger, code, null, text, details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage warn(Logger logger, String code, List<String> arguments, String text, String details) {
        checkNotNull(code);
        return doLogMessage(logger, LogMessage.Type.WARN, code, text, arguments, details);
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage error(Logger logger, String code, String text) {
        return error(logger, code, text, (String)null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage error(Logger logger, String code, List<String> arguments, String text) {
        return error(logger, code, arguments, text, (String)null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage error(Logger logger, String code, String text, Throwable throwable) {
        return error(logger, code, null, text, throwable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage error(Logger logger, String code, List<String> arguments, String text, Throwable throwable) {
        checkNotNull(throwable);
        return error(logger, code, arguments, text, Throwables.getStackTraceAsString(throwable));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage error(Logger logger, String code, String text, String details) {
        return error(logger, code, null, text, details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogMessage error(Logger logger, String code, List<String> arguments, String text, String details) {
        checkNotNull(code);
        return doLogMessage(logger, LogMessage.Type.ERROR, code, text, arguments, details);
    }

    private LogMessage doLogMessage(Logger logger, LogMessage.Type type, String code, String text, List<String> arguments, String details) {

        if (logger != null) {
            String message = text;
            if (details != null) {
                message += "\n" + details;
            }
            switch (type) {
                case DEBUG:
                    logger.debug(message);
                    break;
                case INFO:
                    logger.info(message);
                    break;
                case WARN:
                    logger.warn(message);
                    break;
                case ERROR:
                    logger.error(message);
                    break;
            }
        }

        LogMessage message = new LogMessage();
        message.setType(type);
        message.setCode(code);
        message.setText(text);
        message.setDetails(details);
        message.setArguments(arguments);
        return logMessageRepository.save(message);
    }
}
